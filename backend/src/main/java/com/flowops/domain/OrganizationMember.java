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
 * The join between a user and a tenant, carrying the user's role there.
 *
 * <p>Foreign keys are held as plain UUIDs rather than {@code @ManyToOne}
 * associations: it keeps every query explicitly org-scoped (contract §2 rule 6)
 * and removes any chance of a lazy-load walking into another tenant's row.
 */
@Entity
@Table(name = "organization_members")
public class OrganizationMember {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private Role role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected OrganizationMember() {
        // JPA
    }

    public static OrganizationMember create(UUID userId, UUID organizationId, Role role) {
        OrganizationMember member = new OrganizationMember();
        member.id = UUID.randomUUID();
        member.userId = userId;
        member.organizationId = organizationId;
        member.role = role;
        member.joinedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return member;
    }

    /**
     * Changes this member's role. The row identity, tenant, and join time are
     * immutable; only the role moves (contract §5, team management).
     */
    public void changeRole(Role newRole) {
        this.role = newRole;
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

    public Role getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
