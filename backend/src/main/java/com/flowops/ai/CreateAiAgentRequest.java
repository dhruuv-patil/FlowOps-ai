package com.flowops.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Body for creating an AI agent. {@code name} and {@code instructions} are required;
 * {@code model} is an optional provider model override (null → the AI service default)
 * and {@code tools} is the allowlisted tool names this agent may invoke.
 */
public record CreateAiAgentRequest(
        @NotBlank @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description,
        @NotBlank @Size(min = 1, max = 8000) String instructions,
        @Size(max = 80) String model,
        List<String> tools) {

    public CreateAiAgentRequest {
        name = name == null ? null : name.strip();
        description = description == null || description.isBlank() ? null : description.strip();
        instructions = instructions == null ? null : instructions.strip();
        model = model == null || model.isBlank() ? null : model.strip();
        tools = normalizeTools(tools);
    }

    /** Trim, drop blanks, de-duplicate while preserving order. */
    static List<String> normalizeTools(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> cleaned = new ArrayList<>();
        for (String tool : tools) {
            if (tool == null) {
                continue;
            }
            String name = tool.strip();
            if (!name.isBlank() && seen.add(name)) {
                cleaned.add(name);
            }
        }
        return List.copyOf(cleaned);
    }
}
