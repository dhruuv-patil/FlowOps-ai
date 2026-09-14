package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * One append-only entry in an organization's audit trail. Every column is
 * {@code updatable = false}: the application never rewrites history, and there is no
 * delete path — the table is the tenant's own tamper-evidence surface.
 *
 * <p>Contract §9: no secret ever reaches this row. {@code summary} is composed at the
 * call site from non-secret facts only (role names, provider types, masked last-4
 * hints); webhook tokens, Slack URLs and passwords are excluded by construction.
 *
 * <p>{@code actorUserId} and {@code actorEmail} are null for genuinely anonymous
 * events — a failed inbound-webhook authentication has no principal by design.
 * {@code actorEmail} is a snapshot so the trail stays readable after the account is
 * renamed or deleted.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "actor_email", length = 255, updatable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64, updatable = false)
    private AuditAction action;

    @Column(name = "target_type", length = 40, updatable = false)
    private String targetType;

    @Column(name = "target_id", length = 200, updatable = false)
    private String targetId;

    @Column(name = "summary", length = 500, updatable = false)
    private String summary;

    @Column(name = "ip", length = 64, updatable = false)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
        // JPA
    }

    public static AuditLog create(
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            AuditAction action,
            String targetId,
            String summary,
            String ip) {
        AuditLog log = new AuditLog();
        log.id = UUID.randomUUID();
        log.organizationId = organizationId;
        log.actorUserId = actorUserId;
        log.actorEmail = actorEmail;
        log.action = action;
        log.targetType = action.targetType();
        log.targetId = targetId;
        log.summary = summary;
        log.ip = ip;
        log.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return log;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getSummary() {
        return summary;
    }

    public String getIp() {
        return ip;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
