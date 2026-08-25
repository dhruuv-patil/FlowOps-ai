package com.flowops.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PATCH /api/organizations/current} (contract §5.8).
 *
 * <p>The field is {@code name} — a rename, not a create. The slug is immutable and
 * is never accepted here; sending {@code slug} trips {@code fail-on-unknown-properties}
 * and becomes {@code 400 MALFORMED_REQUEST}.
 */
public record RenameOrganizationRequest(
        @NotBlank(message = "Organization name is required.")
        @Size(min = 2, max = 80,
                message = "Organization name must be between 2 and 80 characters.")
        String name) {

    public RenameOrganizationRequest {
        name = name == null ? null : name.strip();
    }
}
