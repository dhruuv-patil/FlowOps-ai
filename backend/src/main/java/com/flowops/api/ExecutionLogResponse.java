package com.flowops.api;

import com.flowops.domain.ExecutionLog;
import com.flowops.domain.LogLevel;
import java.time.Instant;
import java.util.UUID;

/** One line in a run's log. {@code nodeId} is null for execution-level lines. */
public record ExecutionLogResponse(
        UUID id,
        String nodeId,
        LogLevel level,
        String message,
        int seq,
        Instant createdAt) {

    public static ExecutionLogResponse of(ExecutionLog log) {
        return new ExecutionLogResponse(
                log.getId(),
                log.getNodeId(),
                log.getLevel(),
                log.getMessage(),
                log.getSeq(),
                log.getCreatedAt());
    }
}
