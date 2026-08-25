package com.flowops.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Fails the application startup fast on an unsafe auth configuration, rather than
 * letting it boot and mint forgeable tokens.
 *
 * <p>Three checks (contract §12 + review issue #7):
 * <ol>
 *   <li>{@code JWT_SECRET} present and at least 32 bytes.</li>
 *   <li>{@code JWT_SECRET} is not one of the known committed dev placeholders —
 *       a length check alone would pass the 41-char {@code .env.example} value.</li>
 *   <li>{@code SameSite=None} is never paired with {@code Secure=false} (browsers
 *       drop such a cookie, silently breaking refresh).</li>
 * </ol>
 */
@Component
public class AuthPropertiesValidator {

    private static final int MIN_SECRET_BYTES = 32;

    /**
     * Known-bad values that must never sign a token. Lowercased for comparison.
     * The first is the historical {@code docker-compose}/{@code .env.example}
     * default; the rest are common placeholders.
     */
    private static final Set<String> DENYLISTED_SECRETS = Set.of(
            "change-me-dev-secret-change-me-dev-secret",
            "change-me",
            "changeme",
            "secret",
            "dev-secret",
            "your-secret-here");

    private final AuthProperties properties;

    public AuthPropertiesValidator(AuthProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        String secret = properties.jwtSecret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Generate one with `openssl rand -base64 48` "
                            + "and export it (or set flowops.auth.jwt-secret).");
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short: it must be at least " + MIN_SECRET_BYTES
                            + " bytes. Generate one with `openssl rand -base64 48`.");
        }

        if (DENYLISTED_SECRETS.contains(secret.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "JWT_SECRET is a known placeholder value and must never be used to sign "
                            + "tokens. Generate a real one with `openssl rand -base64 48`.");
        }

        AuthProperties.RefreshCookie cookie = properties.refreshCookie();
        if ("None".equalsIgnoreCase(cookie.sameSite()) && !cookie.secure()) {
            throw new IllegalStateException(
                    "Refresh cookie is misconfigured: SameSite=None requires Secure=true "
                            + "(set REFRESH_COOKIE_SECURE=true).");
        }
    }
}
