package com.flowops.auth;

import com.flowops.api.AuthResponse;

/**
 * An {@link AuthResponse} together with the refresh-token plaintext the controller
 * must write as a cookie.
 *
 * <p>The plaintext is carried out of the service this way — rather than the service
 * touching {@code HttpServletResponse} — so that the "never put the refresh token
 * in a response body" rule is visible in one place: the controller sets a header
 * and serializes {@code response()}, and the two values cannot be confused.
 *
 * <p>{@code refreshToken} is null for switch-org, which deliberately does not
 * rotate the cookie (contract §5.10).
 */
public record AuthResult(AuthResponse response, String refreshToken) {

    public static AuthResult rotating(AuthResponse response, String refreshToken) {
        return new AuthResult(response, refreshToken);
    }

    /** No {@code Set-Cookie} is emitted; the existing refresh cookie stays as-is. */
    public static AuthResult tokenOnly(AuthResponse response) {
        return new AuthResult(response, null);
    }

    public boolean hasRefreshToken() {
        return refreshToken != null;
    }
}
