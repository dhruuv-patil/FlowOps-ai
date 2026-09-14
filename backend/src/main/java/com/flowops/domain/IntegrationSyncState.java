package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Sync state for a provider integration.
 * One row per integration, updated by the sync scheduler.
 */
@Entity
@Table(name = "integration_sync_state")
public class IntegrationSyncState {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "integration_id", nullable = false, updatable = false, unique = true)
    private UUID integrationId;

    @Column(name = "cursor", length = 1000)
    private String cursor;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IntegrationSyncState() {
        // JPA
    }

    public static IntegrationSyncState create(
            UUID organizationId,
            UUID integrationId) {

        IntegrationSyncState state = new IntegrationSyncState();
        state.id = UUID.randomUUID();
        state.organizationId = organizationId;
        state.integrationId = integrationId;
        state.status = "NEW";
        state.attempt = 0;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        state.createdAt = now;
        state.updatedAt = now;
        return state;
    }

    private void touch() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void updateCursor(String cursor) {
        this.cursor = cursor;
        touch();
    }

    public void markSynced() {
        this.lastSyncedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        this.lastSuccessAt = this.lastSyncedAt;
        this.status = "HEALTHY";
        this.attempt = 0;
        touch();
    }

    public void markFailed(String error) {
        this.lastSyncedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        this.lastErrorAt = this.lastSyncedAt;
        this.lastError = error;
        this.status = "FAILED";
        this.attempt = this.attempt + 1;
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

    public String getCursor() {
        return cursor;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getLastErrorAt() {
        return lastErrorAt;
    }

    public String getLastError() {
        return lastError;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempt() {
        return attempt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}