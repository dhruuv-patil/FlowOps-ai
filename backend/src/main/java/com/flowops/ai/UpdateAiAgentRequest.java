package com.flowops.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Body for editing an AI agent. Same shape and rules as {@link CreateAiAgentRequest};
 * every field is replaced with the supplied value.
 */
public record UpdateAiAgentRequest(
        @NotBlank @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description,
        @NotBlank @Size(min = 1, max = 8000) String instructions,
        @Size(max = 80) String model,
        List<String> tools) {

    public UpdateAiAgentRequest {
        name = name == null ? null : name.strip();
        description = description == null || description.isBlank() ? null : description.strip();
        instructions = instructions == null ? null : instructions.strip();
        model = model == null || model.isBlank() ? null : model.strip();
        tools = CreateAiAgentRequest.normalizeTools(tools);
    }
}
