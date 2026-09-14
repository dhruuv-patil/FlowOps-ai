package com.flowops.api;

import com.flowops.domain.Role;
import java.time.Instant;

/**
 * The one-time response to creating/regenerating an invitation: the raw acceptance
 * token. Shown to the admin exactly once — it is not persisted in plaintext and can
 * never be recovered from a later read. The frontend composes the shareable link
 * from its own origin (e.g. {@code {origin}/invite?token=…}); the backend does not
 * need to know the web app's URL to do this.
 */
public record InvitationSecretResponse(
        String token, String email, Role role, String tokenHint, Instant expiresAt) {
}
