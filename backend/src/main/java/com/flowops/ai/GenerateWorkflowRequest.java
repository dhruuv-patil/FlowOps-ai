package com.flowops.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for "Create with AI": a natural-language description the AI service turns into
 * a draft workflow graph. The generated graph is only ever placed on the builder
 * canvas for review — it is never auto-run.
 */
public record GenerateWorkflowRequest(
        @NotBlank @Size(min = 1, max = 4000) String prompt) {

    public GenerateWorkflowRequest {
        prompt = prompt == null ? null : prompt.strip();
    }
}
