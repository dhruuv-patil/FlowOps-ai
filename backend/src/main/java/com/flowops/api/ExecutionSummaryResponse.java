package com.flowops.api;

import com.flowops.domain.ExecutionStatus;
import com.flowops.domain.TriggerType;
import com.flowops.domain.WorkflowExecution;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A run in a list: enough to render a row and link to the detail, without hauling
 * the trigger payload or per-node data. {@code workflowName} is resolved by the
 * service (executions store only the workflow id).
 */
public record ExecutionSummaryResponse(
        UUID id,
        UUID workflowId,
        String workflowName,
        int versionNumber,
        ExecutionStatus status,
        TriggerType triggerType,
        String error,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs) {

    public static ExecutionSummaryResponse of(WorkflowExecution execution, String workflowName) {
        return new ExecutionSummaryResponse(
                execution.getId(),
                execution.getWorkflowId(),
                workflowName,
                execution.getVersionNumber(),
                execution.getStatus(),
                execution.getTriggerType(),
                execution.getError(),
                execution.getCreatedAt(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                durationMs(execution.getStartedAt(), execution.getFinishedAt()));
    }

    static Long durationMs(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }
}
