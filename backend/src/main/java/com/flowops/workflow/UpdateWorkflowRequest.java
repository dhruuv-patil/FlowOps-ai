package com.flowops.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for renaming / re-describing a workflow (metadata only, not the graph). */
public record UpdateWorkflowRequest(
        @NotBlank @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description) {

    public UpdateWorkflowRequest {
        name = name == null ? null : name.strip();
        description = description == null || description.isBlank() ? null : description.strip();
    }
}
