package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * One row per login; survives refresh-token rotation. Its id is the {@code sid}
 * claim.
 *
 * <p>{@code organizationId} is the switchable current tenant. Keeping it here
 * rather than in the refresh cookie is what makes a switched organization survive
 * a page reload: boot refresh reads this row.
 */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip", length = 45)
    private String ip;

    protected AuthSession() {
        // JPA
    }

    public static AuthSession create(UUID userId, UUID organizationId, String userAgent, String ip) {
        AuthSession session = new AuthSession();
        session.id = UUID.randomUUID();
        session.userId = userId;
        session.organizationId = organizationId;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        session.createdAt = now;
        session.lastUsedAt = now;
        session.revokedAt = null;
        session.userAgent = truncate(userAgent, 255);
        session.ip = truncate(ip, 45);
        return session;
    }

    public void touch() {
        this.lastUsedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        }
    }

    /** Called only after membership of the target org has been verified. */
    public void switchOrganization(UUID newOrganizationId) {
        this.organizationId = newOrganizationId;
        touch();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
