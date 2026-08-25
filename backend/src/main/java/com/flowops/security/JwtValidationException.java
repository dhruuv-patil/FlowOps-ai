package com.flowops.security;

/**
 * Distinguishes an expired access token (the frontend's designed refresh trigger,
 * {@code 401 TOKEN_EXPIRED}) from every other invalid-token case
 * ({@code 401 TOKEN_INVALID}).
 */
public class JwtValidationException extends RuntimeException {

    private final boolean expired;

    private JwtValidationException(String message, boolean expired, Throwable cause) {
        super(message, cause);
        this.expired = expired;
    }

    public static JwtValidationException expired(Throwable cause) {
        return new JwtValidationException("Access token has expired.", true, cause);
    }

    public static JwtValidationException invalid(String message) {
        return new JwtValidationException(message, false, null);
    }

    public static JwtValidationException invalid(String message, Throwable cause) {
        return new JwtValidationException(message, false, cause);
    }

    public boolean isExpired() {
        return expired;
    }
}
