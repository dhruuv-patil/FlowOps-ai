package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A single-use, short-lived password reset token.
 *
 * <p>Only the SHA-256 hex of the token is stored — the plaintext exists solely in
 * the email link, so a database dump yields no usable reset tokens.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordResetToken() {
        // JPA
    }

    public static PasswordResetToken create(UUID userId, String tokenHash, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.id = UUID.randomUUID();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt.truncatedTo(ChronoUnit.MILLIS);
        token.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return token;
    }

    /** Marks this token spent. Presenting it again is an error. */
    public void consume() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        this.usedAt = now;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        // Never expose the hash.
        return "PasswordResetToken{id=" + id + ", userId=" + userId + "}";
    }
}