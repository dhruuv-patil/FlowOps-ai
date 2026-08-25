package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.domain.ExecutionNode;
import com.flowops.domain.NodeRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Per-node run state for the execution monitor: status, the attempt count, resolved
 * input and produced output, which output ports fired, timing, and any error.
 */
public record ExecutionNodeResponse(
        UUID id,
        String nodeId,
        String nodeType,
        String label,
        NodeRunStatus status,
        int attempt,
        JsonNode input,
        JsonNode output,
        List<String> activeHandles,
        String error,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs) {

    public static ExecutionNodeResponse of(ExecutionNode node) {
        return new ExecutionNodeResponse(
                node.getId(),
                node.getNodeId(),
                node.getNodeType(),
                node.getLabel(),
                node.getStatus(),
                node.getAttempt(),
                node.getInput(),
                node.getOutput(),
                node.activeHandles(),
                node.getError(),
                node.getStartedAt(),
                node.getFinishedAt(),
                node.getDurationMs());
    }
}
