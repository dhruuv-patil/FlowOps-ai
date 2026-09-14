package com.flowops.integration.sync;

import com.flowops.config.ProviderSyncProperties;
import com.flowops.domain.Integration;
import com.flowops.domain.IntegrationSyncState;
import com.flowops.repository.IntegrationRepository;
import com.flowops.repository.IntegrationSyncStateRepository;
import com.flowops.repository.IntegrationWorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled sync for all connected provider integrations with monitored workflows.
 * Mirrors the resilience pattern of {@link com.flowops.reliability.detect.ReliabilityScheduler}:
 * per-integration failures are swallowed into sync state; the sweep continues.
 */
@Service
public class IntegrationSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntegrationSyncScheduler.class);

    private final IntegrationRepository integrations;
    private final IntegrationWorkflowRepository workflowRepo;
    private final IntegrationSyncStateRepository syncStateRepo;
    private final IntegrationSyncService syncService;
    private final ProviderSyncProperties properties;

    public IntegrationSyncScheduler(
            IntegrationRepository integrations,
            IntegrationWorkflowRepository workflowRepo,
            IntegrationSyncStateRepository syncStateRepo,
            IntegrationSyncService syncService,
            ProviderSyncProperties properties) {
        this.integrations = integrations;
        this.workflowRepo = workflowRepo;
        this.syncStateRepo = syncStateRepo;
        this.syncService = syncService;
        this.properties = properties;
    }

    /**
     * Runs every {@code flowops.providers.sync-delay} (default 1m).
     * Scans all connected integrations that have monitored workflows.
     */
    @Scheduled(fixedDelayString = "${flowops.providers.sync-delay:1m}",
            initialDelayString = "${flowops.providers.sync-delay:1m}")
    public void sweep() {
        List<Integration> connectedIntegrations = findConnectedIntegrationsWithMonitoredWorkflows();
        log.debug("Provider sync sweep: {} integration(s) with monitored workflows", connectedIntegrations.size());

        for (Integration integration : connectedIntegrations) {
            try {
                syncIntegration(integration);
            } catch (Exception e) {
                // Failure already logged and persisted in sync state by the service
                log.debug("Sync failed for integration {}: {}", integration.getId(), e.getMessage());
            }
        }
    }

    private List<Integration> findConnectedIntegrationsWithMonitoredWorkflows() {
        // Find integrations that are connected and have at least one monitored workflow
        // We do this in two queries to avoid a complex join
        List<Integration> connected = integrations.findByStatus(Integration.STATUS_CONNECTED);
        return connected.stream()
                .filter(i -> workflowRepo.existsByIntegrationIdAndMonitoringEnabledTrue(i.getId()))
                .toList();
    }

    @Transactional
    public void syncIntegration(Integration integration) {
        // Create a synthetic principal for the scheduler (org-scoped, no user)
        // FlowOpsPrincipal(userId, sessionId, organizationId, role, email)
        FlowOpsPrincipal schedulerPrincipal = new FlowOpsPrincipal(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), // system user
                UUID.fromString("00000000-0000-0000-0000-000000000000"), // no session
                integration.getOrganizationId(),
                com.flowops.domain.Role.OWNER,
                "system@flowops.internal"
        );
        syncService.sync(schedulerPrincipal, integration.getId());
    }
}