package com.flowops.repository;

import com.flowops.domain.LogLevel;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection for the org-wide log viewer. Spring Data maps the aliased
 * columns of {@link ExecutionLogRepository#searchForOrganization} onto these getters.
 *
 * <p>Unlike the nested per-run log record, each row carries its own run and workflow
 * identity, because the viewer lists lines from many runs side by side.
 */
public interface ExecutionLogRow {

    UUID getId();

    UUID getExecutionId();

    UUID getWorkflowId();

    String getWorkflowName();

    String getNodeId();

    LogLevel getLevel();

    String getMessage();

    int getSeq();

    Instant getCreatedAt();
}
