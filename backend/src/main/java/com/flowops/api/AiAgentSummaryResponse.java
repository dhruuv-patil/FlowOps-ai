package com.flowops.api;

import com.flowops.domain.AiAgent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Row shape for the AI agents list. Omits the (potentially long) instructions;
 * the editor fetches those via the detail endpoint.
 */
public record AiAgentSummaryResponse(
        UUID id,
        String name,
        String description,
        String model,
        List<String> tools,
        Instant createdAt,
        Instant updatedAt) {

    public static AiAgentSummaryResponse of(AiAgent agent, List<String> tools) {
        return new AiAgentSummaryResponse(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getModel(),
                tools,
                agent.getCreatedAt(),
                agent.getUpdatedAt());
    }
}
