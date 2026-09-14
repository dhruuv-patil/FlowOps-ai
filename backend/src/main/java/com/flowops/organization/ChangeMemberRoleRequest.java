package com.flowops.organization;

import com.flowops.domain.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /api/organizations/current/members/{userId}} — change a
 * member's role. The target user is a path variable, never in the body; the tenant
 * is always the caller's own org (from the principal).
 *
 * <p>{@code OWNER} is a permitted target value here — but the service only lets an
 * existing {@code OWNER} grant it, and never lets the last owner be demoted.
 */
public record ChangeMemberRoleRequest(
        @NotNull(message = "A role is required.") Role role) {
}
