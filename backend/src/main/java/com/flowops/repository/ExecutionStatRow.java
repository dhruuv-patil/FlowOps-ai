package com.flowops.repository;

import com.flowops.domain.ExecutionStatus;
import java.time.Instant;

/**
 * Read-only projection for dashboard analytics. Spring Data maps the aliased
 * columns of {@link WorkflowExecutionRepository#statsSince} onto these getters.
 */
public interface ExecutionStatRow {

    ExecutionStatus getStatus();

    Instant getCreatedAt();

    Instant getStartedAt();

    Instant getFinishedAt();
}
