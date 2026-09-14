package com.flowops.organization;

import com.flowops.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Body of {@code POST /api/organizations/current/invitations} — invite an existing
 * user to the caller's org at a role. The tenant is the caller's own org; it is
 * never accepted from the body.
 *
 * <p>{@code role} is validated in the service to be one of {@code ADMIN},
 * {@code MEMBER}, {@code VIEWER}: an {@code OWNER} is only ever minted by founding an
 * organization, never by invitation.
 */
public record InviteMemberRequest(
        @NotBlank(message = "Email is required.")
        @Size(max = 255, message = "Email must be at most 255 characters.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Enter a valid email address.")
        String email,

        @NotNull(message = "A role is required.") Role role) {

    public InviteMemberRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
