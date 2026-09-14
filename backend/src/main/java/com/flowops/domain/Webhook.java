package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A public, JWT-less ingress that lets an external system start a run of one
 * workflow via {@code POST /api/webhooks/{workflowId}/{token}}.
 *
 * <p>The secret token is never held here — only its lowercase-hex SHA-256
 * {@code tokenHash} (verified in constant time) and a non-secret {@code tokenHint}
 * (last-4, safe to display). The full working URL is returned to the admin exactly
 * once, at generate time, and can never be recovered from this row. Foreign keys are
 * plain UUIDs (contract §2 rule 6); the {@code organizationId} recorded here is the
 * trusted tenant the ingress threads into a principal-free run.
 *
 * <p>State transitions are methods rather than public setters so the token hash and
 * hint can only be changed together, through {@link #rotate}.
 */
@Entity
@Table(name = "webhooks")
public class Webhook {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "token_hint", length = 8)
    private String tokenHint;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Webhook() {
        // JPA
    }

    /** Creates a freshly-minted, enabled webhook for a workflow. */
    public static Webhook create(
            UUID organizationId, UUID workflowId, UUID createdBy, String tokenHash, String tokenHint) {
        Webhook webhook = new Webhook();
        webhook.id = UUID.randomUUID();
        webhook.organizationId = organizationId;
        webhook.workflowId = workflowId;
        webhook.createdBy = createdBy;
        webhook.tokenHash = tokenHash;
        webhook.tokenHint = tokenHint;
        webhook.enabled = true;
        Instant now = now();
        webhook.createdAt = now;
        webhook.updatedAt = now;
        return webhook;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private void touch() {
        this.updatedAt = now();
    }

    /**
     * Replaces the stored token with a freshly-minted one and re-enables the webhook —
     * regenerating a URL is an explicit act that should produce a working endpoint.
     */
    public void rotate(String tokenHash, String tokenHint) {
        this.tokenHash = tokenHash;
        this.tokenHint = tokenHint;
        this.enabled = true;
        touch();
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getTokenHint() {
        return tokenHint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
