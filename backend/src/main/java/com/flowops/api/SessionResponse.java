package com.flowops.api;

import com.flowops.domain.Role;
import java.util.List;

/**
 * The identity + tenant snapshot returned by {@code GET /api/auth/me}
 * (contract §4.5).
 *
 * <p>{@code currentOrganization} and {@code currentRole} are non-null in every
 * response: when no org context can be established the request fails with
 * {@code 403 NO_ORGANIZATION_CONTEXT} rather than returning nulls.
 */
public record SessionResponse(
        UserResponse user,
        OrganizationResponse currentOrganization,
        Role currentRole,
        List<MembershipResponse> memberships) {
}
