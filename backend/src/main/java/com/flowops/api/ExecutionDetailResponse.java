package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.domain.ExecutionStatus;
import com.flowops.domain.TriggerType;
import com.flowops.domain.WorkflowExecution;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The full view of one run: its header, the trigger payload, every node's state,
 * and the log. Powers the execution detail page and the initial SSE snapshot.
 */
public record ExecutionDetailResponse(
        UUID id,
        UUID workflowId,
        String workflowName,
        UUID workflowVersionId,
        int versionNumber,
        ExecutionStatus status,
        TriggerType triggerType,
        JsonNode triggerPayload,
        String error,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        List<ExecutionNodeResponse> nodes,
        List<ExecutionLogResponse> logs) {

    public static ExecutionDetailResponse of(
            WorkflowExecution execution,
            String workflowName,
            List<ExecutionNodeResponse> nodes,
            List<ExecutionLogResponse> logs) {
        return new ExecutionDetailResponse(
                execution.getId(),
                execution.getWorkflowId(),
                workflowName,
                execution.getWorkflowVersionId(),
                execution.getVersionNumber(),
                execution.getStatus(),
                execution.getTriggerType(),
                execution.getTriggerPayload(),
                execution.getError(),
                execution.getCreatedAt(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                ExecutionSummaryResponse.durationMs(execution.getStartedAt(), execution.getFinishedAt()),
                nodes,
                logs);
    }
}
