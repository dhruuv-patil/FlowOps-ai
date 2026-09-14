package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.ai.AiServiceClient;
import java.util.List;

/**
 * Result of running an agent (test console or, mirrored, an ai_agent node).
 *
 * <p>{@code configured} is false when the AI service has no provider key: {@code output}
 * is then null and nothing is fabricated. {@code toolCalls} lists the allowlisted tools
 * the model invoked, for transparency.
 */
public record AgentRunResponse(
        boolean configured, String output, List<ToolCall> toolCalls, String model) {

    public record ToolCall(String name, JsonNode arguments, String result) {
    }

    public static AgentRunResponse of(AiServiceClient.AgentRun run) {
        List<ToolCall> calls = run.toolCalls().stream()
                .map(call -> new ToolCall(call.name(), call.arguments(), call.result()))
                .toList();
        return new AgentRunResponse(run.configured(), run.output(), calls, run.model());
    }
}
