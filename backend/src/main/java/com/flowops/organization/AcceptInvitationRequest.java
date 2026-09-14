package com.flowops.organization;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/invitations/accept} — redeem an invitation token for the
 * authenticated caller. The token is the only input; the org and role come from the
 * stored invitation, never from the client. The caller's email must match the
 * invitation's or the redemption fails with an opaque {@code INVITATION_NOT_FOUND}.
 */
public record AcceptInvitationRequest(
        @NotBlank(message = "An invitation token is required.") String token) {
}
