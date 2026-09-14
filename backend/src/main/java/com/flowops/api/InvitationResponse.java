package com.flowops.api;

import com.flowops.domain.Invitation;
import com.flowops.domain.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * A pending/accepted/revoked invitation as shown to an admin (contract §5, team
 * management). Deliberately carries no token: the working URL is returned exactly
 * once by {@link InvitationSecretResponse} at create time and never again.
 */
public record InvitationResponse(
        UUID id,
        String email,
        Role role,
        Invitation.Status status,
        String tokenHint,
        Instant expiresAt,
        Instant createdAt) {

    public static InvitationResponse of(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getTokenHint(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
