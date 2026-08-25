package com.flowops.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for creating a workflow. A new workflow starts as a DRAFT with an empty
 * graph; the builder fills it in afterwards via save-draft.
 */
public record CreateWorkflowRequest(
        @NotBlank @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description) {

    public CreateWorkflowRequest {
        name = name == null ? null : name.strip();
        description = description == null || description.isBlank() ? null : description.strip();
    }
}
