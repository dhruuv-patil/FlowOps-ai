package com.flowops.workflow.templates;

import jakarta.validation.constraints.Size;

/**
 * Body for "use this template". The only thing a caller may choose is the name of
 * the workflow it becomes — the graph comes from the server-side catalogue, and
 * the organization comes from the principal, never from the request.
 */
public record UseTemplateRequest(@Size(max = 120) String name) {

    public UseTemplateRequest {
        name = name == null || name.isBlank() ? null : name.strip();
    }
}
