package com.flowops.auth;

import com.flowops.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Reads and writes the {@code flowops_refresh} cookie (contract §1.2).
 *
 * <p>Uses {@link ResponseCookie} rather than {@link Cookie} because the servlet
 * cookie API has no {@code SameSite} support — it would have to be smuggled in as
 * a fake path attribute.
 */
@Component
public class RefreshCookies {

    private final String name;
    private final String path;
    private final boolean secure;
    private final String sameSite;
    private final Duration ttl;

    public RefreshCookies(AuthProperties properties) {
        AuthProperties.RefreshCookie cookie = properties.refreshCookie();
        this.name = cookie.name();
        this.path = cookie.path();
        this.secure = cookie.secure();
        this.sameSite = cookie.sameSite();
        this.ttl = properties.refreshTokenTtl();
    }

    /** The plaintext token from the request, or empty when the cookie is absent. */
    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /** A {@code Set-Cookie} value carrying a newly issued refresh token. */
    public String issue(String token) {
        return base(token).maxAge(ttl).build().toString();
    }

    /**
     * A {@code Set-Cookie} value that removes the cookie. {@code Path} must match
     * the original exactly or the browser keeps the old cookie.
     */
    public String clear() {
        return base("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite);
        // Domain is deliberately omitted, making this a host-only cookie.
    }

    /** The header name these values are written under. */
    public static String header() {
        return HttpHeaders.SET_COOKIE;
    }
}
