package com.flowops.api;

import com.flowops.domain.Role;
import com.flowops.repository.MembershipRow;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape for one of the caller's memberships (contract §4.3).
 *
 * <p>Denormalized on purpose so the org switcher needs no second request.
 * Arrays of these are always sorted {@code joinedAt ASC, organizationId ASC}.
 */
public record MembershipResponse(
        UUID organizationId,
        String organizationName,
        String organizationSlug,
        Role role,
        Instant joinedAt) {

    public static MembershipResponse of(MembershipRow row) {
        return new MembershipResponse(
                row.organizationId(),
                row.organizationName(),
                row.organizationSlug(),
                row.role(),
                row.joinedAt());
    }
}
