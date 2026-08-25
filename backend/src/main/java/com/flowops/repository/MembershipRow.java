package com.flowops.repository;

import com.flowops.domain.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * Denormalized membership row: one join across {@code organization_members} and
 * {@code organizations}, so the org switcher needs no second request.
 *
 * <p>Top-level (not nested) because JPQL constructor expressions name the class by
 * its binary name, and a nested record would need the {@code $} form.
 */
public record MembershipRow(
        UUID organizationId,
        String organizationName,
        String organizationSlug,
        Role role,
        Instant joinedAt) {
}
