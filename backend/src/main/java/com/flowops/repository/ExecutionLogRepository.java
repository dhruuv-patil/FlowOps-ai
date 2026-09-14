package com.flowops.repository;

import com.flowops.domain.ExecutionLog;
import com.flowops.domain.LogLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, UUID> {

    List<ExecutionLog> findByExecutionIdOrderBySeqAsc(UUID executionId);

    /** Used to seed the per-run seq counter when resuming an execution. */
    int countByExecutionId(UUID executionId);

    /**
     * Org-wide log viewer. {@code execution_logs} deliberately carries no
     * {@code organization_id} — it is append-only run detail — so tenancy is resolved
     * by joining through {@code workflow_executions} (which does) and on to
     * {@code workflows} for the display name. Nothing is denormalized and the
     * append-only entity shape stays intact.
     *
     * <p>Foreign keys are plain UUIDs (contract §2 rule 6), so these are explicit theta
     * joins rather than association navigation. Optional {@code workflowId} and
     * {@code level} filters are disabled by passing null; the caller supplies a
     * {@link Pageable} to cap the row count.
     */
    @Query("""
            SELECT l.id AS id,
                   l.executionId AS executionId,
                   e.workflowId AS workflowId,
                   w.name AS workflowName,
                   l.nodeId AS nodeId,
                   l.level AS level,
                   l.message AS message,
                   l.seq AS seq,
                   l.createdAt AS createdAt
            FROM ExecutionLog l, WorkflowExecution e, Workflow w
            WHERE l.executionId = e.id
              AND e.workflowId = w.id
              AND e.organizationId = :organizationId
              AND (:workflowId IS NULL OR e.workflowId = :workflowId)
              AND (:level IS NULL OR l.level = :level)
            ORDER BY l.createdAt DESC, l.seq DESC
            """)
    List<ExecutionLogRow> searchForOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("workflowId") UUID workflowId,
            @Param("level") LogLevel level,
            Pageable pageable);
}
