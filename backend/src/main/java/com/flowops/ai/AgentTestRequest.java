package com.flowops.ai;

import jakarta.validation.constraints.Size;

/**
 * Body for the agent test console: the user input to run the agent against. Optional —
 * an agent whose instructions are self-contained can be run with no input.
 */
public record AgentTestRequest(@Size(max = 8000) String input) {

    public AgentTestRequest {
        input = input == null || input.isBlank() ? null : input.strip();
    }
}
