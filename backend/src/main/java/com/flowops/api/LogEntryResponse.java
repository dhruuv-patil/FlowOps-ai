package com.flowops.api;

import com.flowops.domain.LogLevel;
import com.flowops.repository.ExecutionLogRow;
import java.time.Instant;
import java.util.UUID;

/**
 * One line in the org-wide log viewer. Unlike {@link ExecutionLogResponse} (which is
 * nested inside a single run's detail and therefore needs no identity) each row here
 * carries its own run and workflow, because the viewer interleaves lines from many runs.
 *
 * <p>Contract §9: log messages are written by the engine and executors from outcomes and
 * user-safe detail only, so there is no secret to mask at this layer.
 */
public record LogEntryResponse(
        UUID id,
        UUID executionId,
        UUID workflowId,
        String workflowName,
        String nodeId,
        LogLevel level,
        String message,
        int seq,
        Instant createdAt) {

    public static LogEntryResponse of(ExecutionLogRow row) {
        return new LogEntryResponse(
                row.getId(),
                row.getExecutionId(),
                row.getWorkflowId(),
                row.getWorkflowName(),
                row.getNodeId(),
                row.getLevel(),
                row.getMessage(),
                row.getSeq(),
                row.getCreatedAt());
    }
}
