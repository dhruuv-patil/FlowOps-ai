package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * One row per rotation generation of a session's refresh token.
 *
 * <p>Only the SHA-256 hex of the token is stored — the plaintext exists solely in
 * the HttpOnly cookie, so a database dump yields no usable refresh tokens.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
        // JPA
    }

    public static RefreshToken create(UUID sessionId, String tokenHash, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.sessionId = sessionId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt.truncatedTo(ChronoUnit.MILLIS);
        token.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return token;
    }

    /** Marks this generation spent. Presenting it again is treated as theft. */
    public void consume() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        this.usedAt = now;
        this.revokedAt = now;
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        }
    }

    public boolean isSpent() {
        return usedAt != null || revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        // Never expose the hash, let alone anything derived from the plaintext.
        return "RefreshToken{id=" + id + ", sessionId=" + sessionId + "}";
    }
}
