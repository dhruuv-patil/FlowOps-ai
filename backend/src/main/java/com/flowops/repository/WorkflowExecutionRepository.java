package com.flowops.repository;

import com.flowops.domain.ExecutionStatus;
import com.flowops.domain.WorkflowExecution;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Executions, always fetched together with their owning organization id: there is
 * no bare {@code findById} for callers, so cross-tenant reads are impossible by
 * construction (contract §2 rule 6).
 */
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Optional<WorkflowExecution> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Org-scoped list with optional {@code workflowId} and {@code status} filters
     * (nulls disable the respective clause), newest first. The caller passes a
     * {@link Pageable} to cap the page size.
     */
    @Query("""
            SELECT e FROM WorkflowExecution e
            WHERE e.organizationId = :organizationId
              AND (:workflowId IS NULL OR e.workflowId = :workflowId)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.createdAt DESC
            """)
    List<WorkflowExecution> search(
            @Param("organizationId") UUID organizationId,
            @Param("workflowId") UUID workflowId,
            @Param("status") ExecutionStatus status,
            Pageable pageable);

    /**
     * Lightweight projection over a time window for the dashboard: just the fields
     * the KPI and time-series computation needs, so a wide range does not haul full
     * trigger payloads into memory.
     */
    @Query("""
            SELECT e.status AS status,
                   e.createdAt AS createdAt,
                   e.startedAt AS startedAt,
                   e.finishedAt AS finishedAt
            FROM WorkflowExecution e
            WHERE e.organizationId = :organizationId
              AND e.createdAt >= :since
            """)
    List<ExecutionStatRow> statsSince(
            @Param("organizationId") UUID organizationId, @Param("since") Instant since);
}
