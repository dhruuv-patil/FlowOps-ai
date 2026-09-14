package com.flowops.integration;

import com.flowops.domain.Integration;
import com.flowops.domain.IntegrationWorkflow;
import com.flowops.repository.IntegrationWorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing external workflow mappings.
 */
@Service
public class IntegrationWorkflowService {

    private final IntegrationWorkflowRepository repo;

    public IntegrationWorkflowService(IntegrationWorkflowRepository repo) {
        this.repo = repo;
    }

    /**
     * Gets an existing workflow mapping or creates a new one.
     * Does not enable monitoring — that's a separate step.
     */
    @Transactional
    public IntegrationWorkflow getOrCreate(
            Integration integration,
            String providerWorkflowId,
            Map<String, Object> metadata) {

        return repo.findByIntegrationIdAndProviderWorkflowId(
                integration.getId(), providerWorkflowId)
                .orElseGet(() -> {
                    String name = extractName(metadata, providerWorkflowId);

                    IntegrationWorkflow wf = IntegrationWorkflow.create(
                            integration.getOrganizationId(),
                            integration.getId(),
                            providerWorkflowId,
                            name);

                    wf.updateFromProvider(name, null, metadata);

                    return repo.save(wf);
                });
    }

    @Transactional(readOnly = true)
    public Optional<IntegrationWorkflow> findById(
            FlowOpsPrincipal principal,
            UUID id) {

        return repo.findByIdAndOrganizationId(
                id,
                principal.organizationId());
    }

    @Transactional(readOnly = true)
    public Optional<IntegrationWorkflow> findByIdAndOrganizationId(
            UUID id,
            UUID orgId) {

        return repo.findByIdAndOrganizationId(id, orgId);
    }

    /**
     * Finds a workflow mapping using the provider's workflow ID.
     *
     * Provider workflow IDs are strings and must not be treated as
     * FlowOps database UUIDs. For example, n8n IDs can look like:
     * "9lgH0GD7XEbnLE82".
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationWorkflow> findByIntegrationIdAndProviderWorkflowId(
            UUID integrationId,
            String providerWorkflowId) {

        return repo.findByIntegrationIdAndProviderWorkflowId(
                integrationId,
                providerWorkflowId);
    }

    private String extractName(
            Map<String, Object> metadata,
            String fallback) {

        if (metadata != null) {
            Object name = metadata.get("name");

            if (name instanceof String s && !s.isBlank()) {
                return s;
            }
        }

        return fallback;
    }

    @Transactional
    public IntegrationWorkflow save(IntegrationWorkflow workflow) {
        return repo.save(workflow);
    }

    @Transactional(readOnly = true)
    public List<IntegrationWorkflow> findByIntegrationIdAndMonitoringEnabledTrue(
            UUID integrationId) {

        return repo.findByIntegrationIdAndMonitoringEnabledTrue(
                integrationId);
    }
}