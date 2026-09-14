package com.flowops.api;

import com.flowops.domain.AiAgent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full AI agent including its instructions. Returned by get/create/update. */
public record AiAgentDetailResponse(
        UUID id,
        String name,
        String description,
        String instructions,
        String model,
        List<String> tools,
        Instant createdAt,
        Instant updatedAt) {

    public static AiAgentDetailResponse of(AiAgent agent, List<String> tools) {
        return new AiAgentDetailResponse(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getInstructions(),
                agent.getModel(),
                tools,
                agent.getCreatedAt(),
                agent.getUpdatedAt());
    }
}
