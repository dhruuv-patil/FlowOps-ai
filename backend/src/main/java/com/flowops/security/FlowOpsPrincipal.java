package com.flowops.security;

import com.flowops.domain.Role;
import java.util.UUID;

/**
 * The authenticated caller, built from verified JWT claims and placed in the
 * {@code SecurityContext}. Controllers obtain the tenant context only from here
 * (via {@link AuthenticatedUser}), never from a request body or query parameter.
 *
 * @param userId         the {@code sub} claim — the principal
 * @param sessionId      the {@code sid} claim — FK to {@code auth_sessions}
 * @param organizationId the {@code orgId} claim — the sole source of truth for the tenant
 * @param role           the caller's role in {@code organizationId}
 * @param email          convenience claim only; never an authorization input
 */
public record FlowOpsPrincipal(
        UUID userId,
        UUID sessionId,
        UUID organizationId,
        Role role,
        String email) {
}
