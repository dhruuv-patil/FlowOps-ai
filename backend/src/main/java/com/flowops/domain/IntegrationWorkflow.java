package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps an external workflow (from a connected provider) to FlowOps.
 *
 * <p>This is the primary entity for external workflow monitoring.
 * {@code workflow_id} is nullable — it is only set when the user later
 * creates a FlowOps-native workflow representation. The light path
 * (n8n workflow → telemetry directly) keeps this null.
 */
@Entity
@Table(name = "integration_workflows")
public class IntegrationWorkflow {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "integration_id", nullable = false, updatable = false)
    private UUID integrationId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "provider_workflow_id", nullable = false, length = 255, updatable = false)
    private String providerWorkflowId;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "monitoring_enabled", nullable = false)
    private boolean monitoringEnabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IntegrationWorkflow() {
        // JPA
    }

    public static IntegrationWorkflow create(
            UUID organizationId,
            UUID integrationId,
            String providerWorkflowId,
            String name) {

        IntegrationWorkflow ew = new IntegrationWorkflow();
        ew.id = UUID.randomUUID();
        ew.organizationId = organizationId;
        ew.integrationId = integrationId;
        ew.providerWorkflowId = providerWorkflowId;
        ew.name = name;
        ew.monitoringEnabled = false;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ew.createdAt = now;
        ew.updatedAt = now;
        return ew;
    }

    private void touch() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void enableMonitoring() {
        this.monitoringEnabled = true;
        touch();
    }

    public void disableMonitoring() {
        this.monitoringEnabled = false;
        touch();
    }

    public void updateFromProvider(String name, String status, Map<String, Object> metadata) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        this.status = status;
        this.metadata = metadata;
        touch();
    }

    public void markSynced() {
        this.lastSyncedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        touch();
    }

    public void linkWorkflow(UUID workflowId) {
        this.workflowId = workflowId;
        touch();
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getIntegrationId() {
        return integrationId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getProviderWorkflowId() {
        return providerWorkflowId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}