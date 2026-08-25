package com.flowops.api;

import com.flowops.domain.Role;
import com.flowops.repository.OrganizationMemberRow;
import java.time.Instant;
import java.util.UUID;

/**
 * A member of the current organization (contract §4.4).
 *
 * <p>Readable by any role including {@code VIEWER}: a member list inside your own
 * tenant is not privileged. Mutating it is, and that lands in M5.
 */
public record OrganizationMemberResponse(
        UUID userId,
        String email,
        String fullName,
        String avatarUrl,
        Role role,
        Instant joinedAt) {

    public static OrganizationMemberResponse of(OrganizationMemberRow row) {
        return new OrganizationMemberResponse(
                row.userId(),
                row.email(),
                row.fullName(),
                row.avatarUrl(),
                row.role(),
                row.joinedAt());
    }
}
