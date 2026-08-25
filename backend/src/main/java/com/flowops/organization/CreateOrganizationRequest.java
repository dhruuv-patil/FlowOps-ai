package com.flowops.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/organizations} (contract §5.9).
 *
 * <p>The field is {@code organizationName}, matching register rather than the
 * {@code name} of a rename — so one Zod fragment serves both create forms. The
 * caller is made {@code OWNER}; there is no role field to negotiate.
 */
public record CreateOrganizationRequest(
        @NotBlank(message = "Organization name is required.")
        @Size(min = 2, max = 80,
                message = "Organization name must be between 2 and 80 characters.")
        String organizationName) {

    public CreateOrganizationRequest {
        organizationName = organizationName == null ? null : organizationName.strip();
    }
}
