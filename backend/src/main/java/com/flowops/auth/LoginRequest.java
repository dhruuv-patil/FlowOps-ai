package com.flowops.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * {@code POST /api/auth/login} body (contract §5.2, §7.1).
 *
 * <p><strong>Login checks presence and maximum length only.</strong> It must never
 * apply the password <em>complexity</em> rule: rejecting a credential for failing
 * it would tell an attacker that the stored password has a different form from
 * what the current policy requires. Complexity is enforced at registration, where
 * it belongs.
 *
 * <p>The 72-character cap is still applied here — it bounds work, so an attacker
 * cannot force expensive BCrypt calls with a megabyte password.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Size(max = 255, message = "Email must be at most 255 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(max = 72, message = "Password must be at most 72 characters.")
        String password) {

    public LoginRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        // password is intentionally NOT trimmed.
    }
}
