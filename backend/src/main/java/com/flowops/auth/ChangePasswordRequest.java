package com.flowops.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * {@code POST /api/auth/change-password} body (Slice 3, security settings).
 *
 * <p>{@code currentPassword} is only checked for presence — it is verified against
 * the stored BCrypt hash in the service, and any strength rule on it belongs to
 * whenever it was first set, not now. {@code newPassword} carries the same
 * {@code @Pattern} as {@link RegisterRequest} so the two entry points cannot drift.
 *
 * <p>Neither field is trimmed: leading/trailing whitespace is a legitimate part of
 * a passphrase, matching {@link RegisterRequest}.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message =
                        "Password must be 8–72 characters and include at least one letter"
                                + " and one number.")
        String newPassword) {
}
