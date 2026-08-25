package com.flowops.repository;

import com.flowops.domain.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * Denormalized member row: {@code organization_members} joined to {@code users},
 * so the members list of the current org (contract §5.11) needs no second query.
 *
 * <p>Top-level (not nested) because JPQL constructor expressions name the class by
 * its binary name.
 */
public record OrganizationMemberRow(
        UUID userId,
        String email,
        String fullName,
        String avatarUrl,
        Role role,
        Instant joinedAt) {
}
