package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

/**
 * A tokened, one-time invitation for an existing user to join an organization at a
 * chosen role. Mapped to {@code invitations} (Flyway {@code V7__invitations.sql}).
 *
 * <p>Foreign keys are plain UUIDs, matching the rest of the domain (contract §2
 * rule 6). Only the token's SHA-256 hash is stored — never the token — so a
 * database read can never recover a working invite URL. The role is constrained by
 * a DB check to {@code ADMIN}/{@code MEMBER}/{@code VIEWER}: an {@code OWNER} is only
 * ever created by founding an organization.
 */
@Entity
@Table(name = "invitations")
public class Invitation {

    /** Lifecycle of an invitation. */
    public enum Status {
        PENDING,
        ACCEPTED,
        REVOKED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private Role role;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "token_hint", length = 8)
    private String tokenHint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Invitation() {
        // JPA
    }

    public static Invitation create(
            UUID organizationId,
            String email,
            Role role,
            String tokenHash,
            String tokenHint,
            UUID createdBy,
            Instant expiresAt) {
        Invitation invitation = new Invitation();
        invitation.id = UUID.randomUUID();
        invitation.organizationId = organizationId;
        invitation.email = email.toLowerCase(Locale.ROOT);
        invitation.role = role;
        invitation.tokenHash = tokenHash;
        invitation.tokenHint = tokenHint;
        invitation.status = Status.PENDING;
        invitation.expiresAt = expiresAt;
        invitation.createdBy = createdBy;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        invitation.createdAt = now;
        invitation.updatedAt = now;
        return invitation;
    }

    /** Re-mints a pending invitation's token (and optionally its role), extending expiry. */
    public void reissue(Role role, String tokenHash, String tokenHint, Instant expiresAt) {
        this.role = role;
        this.tokenHash = tokenHash;
        this.tokenHint = tokenHint;
        this.status = Status.PENDING;
        this.expiresAt = expiresAt;
        touch();
    }

    public void accept() {
        this.status = Status.ACCEPTED;
        touch();
    }

    public void revoke() {
        this.status = Status.REVOKED;
        touch();
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    private void touch() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getTokenHint() {
        return tokenHint;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
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
