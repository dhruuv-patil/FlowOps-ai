package com.flowops.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * {@code POST /api/auth/reset-password} body.
 *
 * <p>Token is the raw reset token from the email link (not hashed). The new
 * password carries the same complexity rule as {@link ForgotPasswordRequest} so
 * the two entry points cannot drift.
 *
 * <p>Neither field is trimmed: leading/trailing whitespace is a legitimate part
 * of a passphrase/token.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required.")
        String token,

        @NotBlank(message = "New password is required.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message =
                        "Password must be 8–72 characters and include at least one letter"
                                + " and one number.")
        String newPassword) {
}