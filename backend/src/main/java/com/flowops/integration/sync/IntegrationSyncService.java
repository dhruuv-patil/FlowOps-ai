package com.flowops.integration.sync;

import com.flowops.api.ExecutionEventEnvelopes;
import com.flowops.common.crypto.CredentialCipher;
import com.flowops.domain.Integration;
import com.flowops.domain.IntegrationCredential;
import com.flowops.domain.IntegrationSyncState;
import com.flowops.domain.IntegrationWorkflow;
import com.flowops.execution.ExecutionEventService;
import com.flowops.integration.provider.DecryptedCredentials;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExternalWorkflow;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.ProviderAuthenticationException;
import com.flowops.integration.provider.ProviderCapabilities;
import com.flowops.integration.provider.ProviderConfigurationException;
import com.flowops.integration.provider.ProviderConnectionException;
import com.flowops.integration.provider.ProviderRateLimitException;
import com.flowops.integration.provider.ProviderUnavailableException;
import com.flowops.integration.provider.SyncCursor;
import com.flowops.integration.provider.WorkflowProvider;
import com.flowops.integration.provider.WorkflowProviderRegistry;
import com.flowops.integration.IntegrationWorkflowService;
import com.flowops.reliability.detect.ReliabilityDetectionService;
import com.flowops.reliability.telemetry.TelemetryCollector;
import com.flowops.repository.IntegrationCredentialRepository;
import com.flowops.repository.IntegrationRepository;
import com.flowops.repository.IntegrationSyncStateRepository;
import com.flowops.repository.IntegrationWorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates incremental sync for a provider integration.
 *
 * <p>Flow per integration:
 * 1. Load integration + sync state (org-scoped)
 * 2. Decrypt credentials once
 * 3. Resolve provider from registry
 * 4. Page through executions (fetchExecutions with cursor)
 * 5. For each execution whose workflow is monitored: fetch node executions
 * 6. Normalize and ingest as ExecutionEvents (idempotent via composite unique key)
 * 7. Track touched external workflows and new run IDs
 * 8. Only after page fully succeeds: persist cursor + status = HEALTHY
 * 9. For touched workflows: TelemetryCollector.captureExternal(...)
 * 10. For new runs: ReliabilityDetectionService.detectInExecution(...)
 * 11. On failure: mark sync state FAILED, integration stays CONNECTED, bounded backoff
 */
@Service
public class IntegrationSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(IntegrationSyncService.class);

    private final IntegrationRepository integrations;
    private final IntegrationCredentialRepository credentialsRepo;
    private final IntegrationWorkflowRepository workflowRepo;
    private final IntegrationSyncStateRepository syncStateRepo;
    private final CredentialCipher cipher;
    private final WorkflowProviderRegistry providerRegistry;
    private final IntegrationWorkflowService workflowService;
    private final ExecutionEventService eventService;
    private final TelemetryCollector telemetryCollector;
    private final ReliabilityDetectionService detectionService;

    public IntegrationSyncService(
            IntegrationRepository integrations,
            IntegrationCredentialRepository credentialsRepo,
            IntegrationWorkflowRepository workflowRepo,
            IntegrationSyncStateRepository syncStateRepo,
            CredentialCipher cipher,
            WorkflowProviderRegistry providerRegistry,
            IntegrationWorkflowService workflowService,
            ExecutionEventService eventService,
            TelemetryCollector telemetryCollector,
            ReliabilityDetectionService detectionService) {

        this.integrations = integrations;
        this.credentialsRepo = credentialsRepo;
        this.workflowRepo = workflowRepo;
        this.syncStateRepo = syncStateRepo;
        this.cipher = cipher;
        this.providerRegistry = providerRegistry;
        this.workflowService = workflowService;
        this.eventService = eventService;
        this.telemetryCollector = telemetryCollector;
        this.detectionService = detectionService;
    }

    /**
     * Runs a sync for the given integration.
     *
     * <p>Intended for the scheduler (fire-and-forget, swallows exceptions)
     * and for manual triggers (returns summary).
     */
    @Transactional
    public SyncResult sync(
            FlowOpsPrincipal principal,
            UUID integrationId) {

        UUID orgId = principal.organizationId();

        Integration integration =
                integrations.findByIdAndOrganizationId(
                                integrationId,
                                orgId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Integration not found"));

        IntegrationSyncState syncState =
                syncStateRepo
                        .findByOrganizationIdAndIntegrationId(
                                orgId,
                                integrationId)
                        .orElseGet(() -> {
                            IntegrationSyncState state =
                                    IntegrationSyncState.create(
                                            orgId,
                                            integrationId);
                            return syncStateRepo.save(state);
                        });

        // Get monitored workflows
        List<IntegrationWorkflow> monitored =
                workflowRepo
                        .findByIntegrationIdAndMonitoringEnabledTrue(
                                integrationId);

        if (monitored.isEmpty()) {
            syncState.markSynced();
            syncStateRepo.save(syncState);

            return new SyncResult(
                    0,
                    0,
                    0,
                    0,
                    "NO_MONITORED_WORKFLOWS");
        }

        Set<String> monitoredExternalIds =
                new HashSet<>();

        for (IntegrationWorkflow workflow : monitored) {
            monitoredExternalIds.add(
                    workflow.getProviderWorkflowId());
        }

        // Decrypt credentials once
        IntegrationCredential credEntity =
                credentialsRepo
                        .findByIntegrationId(integrationId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No credential for integration"));

        Map<String, String> decrypted =
                cipher.decryptToMap(
                        credEntity.getCiphertext());

        DecryptedCredentials creds =
                new DecryptedCredentials(decrypted);

        // Resolve provider
        WorkflowProvider provider =
                providerRegistry.getByWire(
                        integration.getType());

        // Sync loop
        SyncCursor cursor =
                new SyncCursor(
                        syncState.getCursor() != null
                                ? syncState.getCursor()
                                : "");

        int totalWorkflows = 0;
        int totalExecutions = 0;
        int totalEvents = 0;
        int duplicates = 0;

        Set<UUID> touchedExternalWorkflowRefs =
                new HashSet<>();

        List<UUID> newRunUuids =
                new ArrayList<>();

        try {

            while (true) {

                var page =
                        provider.fetchExecutions(
                                new IntegrationContext(
                                        integrationId,
                                        orgId,
                                        creds),
                                cursor);

                if (page.items().isEmpty()) {
                    break;
                }

                for (ExternalExecution exec : page.items()) {

                    if (!monitoredExternalIds.contains(
                            exec.workflowExternalId())) {

                        continue;
                    }

                    // Get or create the external workflow mapping
                    IntegrationWorkflow extWf =
                            workflowService.getOrCreate(
                                    integration,
                                    exec.workflowExternalId(),
                                    exec.metadata());

                    totalWorkflows++;

                    // Fetch node executions
                    List<ExternalNodeExecution> nodes;

                    try {

                        nodes =
                                provider.fetchNodeExecutions(
                                        new IntegrationContext(
                                                integrationId,
                                                orgId,
                                                creds),
                                        exec);

                    } catch (Exception e) {

                        log.warn(
                                "Failed to fetch node executions for {}: {}",
                                exec.externalId(),
                                e.getMessage());

                        continue;
                    }

                    // Ingest events
                    var batchRequest =
                            buildBatchRequest(
                                    integration,
                                    extWf,
                                    exec,
                                    nodes);

                    var batchResult =
                            eventService.ingestBatch(
                                    principal,
                                    batchRequest);

                    totalExecutions++;

                    totalEvents +=
                            batchResult.accepted();

                    duplicates +=
                            batchResult.rejected();

                    // Track for post-sync processing
                    touchedExternalWorkflowRefs.add(
                            extWf.getId());

                    // Track new run UUIDs for detection
                    //
                    // Run UUID is deterministic:
                    // nameUUIDFromBytes(
                    //     integrationId + ":" + executionExternalId
                    // )
                    UUID runUuid =
                            uuidFromParts(
                                    integrationId,
                                    exec.externalId());

                    newRunUuids.add(runUuid);
                }

                // Only advance cursor after the full page succeeded
                cursor = page.nextCursor();

                if (!page.hasMore()) {
                    break;
                }
            }

            // Persist cursor and mark healthy
            //
            // Capture previous success timestamp BEFORE updating.
            // First sync has no prior success.

            Instant previousSuccessAt =
                    syncState.getLastSuccessAt();

            syncState.updateCursor(
                    cursor.value());

            syncState.markSynced();

            syncStateRepo.save(syncState);

            // Update last_synced_at on touched workflows
            for (UUID extWfId :
                    touchedExternalWorkflowRefs) {

                workflowRepo
                        .findById(extWfId)
                        .ifPresent(w -> {
                            w.markSynced();
                            workflowRepo.save(w);
                        });
            }

            // Telemetry + detection for new runs.
            //
            // Use previousSuccessAt so we include events just
            // ingested during this sync.
            //
            // On first sync previousSuccessAt is null, so
            // captureExternal reads all events since epoch.

            for (UUID extWfRef :
                    touchedExternalWorkflowRefs) {

                try {

                    telemetryCollector.captureExternal(
                            extWfRef,
                            previousSuccessAt);

                } catch (Exception e) {

                    log.warn(
                            "External telemetry capture failed for {}: {}",
                            extWfRef,
                            e.getMessage());
                }
            }

            for (UUID runUuid : newRunUuids) {

                try {

                    detectionService.detectInExecution(
                            runUuid);

                } catch (Exception e) {

                    log.warn(
                            "External detection failed for {}: {}",
                            runUuid,
                            e.getMessage());
                }
            }

            return new SyncResult(
                    touchedExternalWorkflowRefs.size(),
                    totalExecutions,
                    totalEvents,
                    duplicates,
                    "COMPLETED");

        } catch (ProviderAuthenticationException e) {

            // Auth failure:
            // transition integration to auth_error.
            //
            // Integration stays connected until an ADMIN
            // re-tests credentials.
            //
            // A temporary 5xx never takes this path.

            integration.markAuthError();

            integrations.save(integration);

            syncState.markFailed(
                    "Authentication failed: "
                            + e.getMessage());

            syncStateRepo.save(syncState);

            throw e;

        } catch (ProviderRateLimitException e) {

            syncState.markFailed(
                    "Rate limited: "
                            + e.getMessage());

            syncStateRepo.save(syncState);

            throw e;

        } catch (
                ProviderUnavailableException
                        | ProviderConfigurationException
                        | ProviderConnectionException e) {

            syncState.markFailed(
                    e.getMessage());

            syncStateRepo.save(syncState);

            throw e;

        } catch (Exception e) {

            log.error(
                    "Sync failed for integration {}: {}",
                    integrationId,
                    e.getMessage(),
                    e);

            syncState.markFailed(
                    "Unexpected error: "
                            + e.getMessage());

            syncStateRepo.save(syncState);

            throw e;
        }
    }

    private ExecutionEventEnvelopes.BatchIngestRequest buildBatchRequest(
            Integration integration,
            IntegrationWorkflow extWf,
            ExternalExecution exec,
            List<ExternalNodeExecution> nodes) {

        List<ExecutionEventEnvelopes.IngestExecutionEventRequest> events =
                new ArrayList<>();

        for (ExternalNodeExecution node : nodes) {

            events.add(
                    new ExecutionEventEnvelopes.IngestExecutionEventRequest(

                            integration.getType(),

                            extWf.getProviderWorkflowId(),

                            exec.externalId(),

                            node.externalId(),

                            node.name(),

                            node.status().name(),

                            node.startedAt() != null
                                    ? node.startedAt()
                                    : Instant.now(),

                            node.startedAt(),

                            node.finishedAt(),

                            durationMs(
                                    node.startedAt(),
                                    node.finishedAt()),

                            0, // retryCount - not available from n8n list API

                            node.inputSize() != null
                                    ? node.inputSize().intValue()
                                    : null,

                            node.outputSize() != null
                                    ? node.outputSize().intValue()
                                    : null,

                            // FIX:
                            // Avoid dependency on a non-existent
                            // ExecutionStatus enum.
                            node.status() != null
                                    && "FAILED".equalsIgnoreCase(
                                            node.status().name())
                                    ? "execution_failed"
                                    : null,

                            null, // errorMessage - bounded later

                            node.metadata(),

                            extWf.getName(),

                            integration.getId(),
                            // Idempotency identity:
                            // (integration, workflow, exec, step)

                            extWf.getId()
                            // Light path:
                            // event.workflow_id = IntegrationWorkflow.id
                    ));
        }

        return new ExecutionEventEnvelopes.BatchIngestRequest(
                events);
    }

    private Long durationMs(
            Instant start,
            Instant finish) {

        if (start != null && finish != null) {

            return finish.toEpochMilli()
                    - start.toEpochMilli();
        }

        return null;
    }

    private UUID uuidFromParts(
            UUID integrationId,
            String executionExternalId) {

        String combined =
                integrationId.toString()
                        + ":"
                        + executionExternalId;

        return UUID.nameUUIDFromBytes(
                combined.getBytes());
    }

    public record SyncResult(
            int workflows,
            int executions,
            int events,
            int duplicates,
            String status) {
    }
}