package com.flowops.api;

import com.flowops.domain.Organization;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape for an organization (contract §4.2).
 *
 * <p>{@code name} is not unique across tenants; {@code slug} is globally unique
 * and server-derived. Clients never send a slug and must not predict one.
 */
public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        Instant createdAt,
        Instant updatedAt) {

    public static OrganizationResponse of(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}
