package com.flowops.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * {@code POST /api/auth/forgot-password} body.
 *
 * <p>The email is normalized identically to {@link LoginRequest}: trimmed and
 * lowercased. Validation is presence-only (no regex) to avoid leaking the stored
 * format — a user entering {@code User@Example.COM} must get the same generic
 * response as someone entering {@code nobody@example.com}.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required.")
        @Size(max = 255, message = "Email must be at most 255 characters.")
        String email) {

    public ForgotPasswordRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}