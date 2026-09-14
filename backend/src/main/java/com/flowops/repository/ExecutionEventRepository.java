package com.flowops.repository;

import com.flowops.domain.ExecutionEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for execution events ingested from external systems.
 * All reads are org-scoped by construction.
 */
public interface ExecutionEventRepository extends JpaRepository<ExecutionEvent, UUID> {

    /** Find by event ID and organization (tenant isolation). */
    Optional<ExecutionEvent> findByIdAndOrganizationId(
            UUID id,
            UUID organizationId);

    /** Find by execution external ID and organization. */
    Optional<ExecutionEvent> findByExecutionExternalIdAndOrganizationId(
            @Param("executionExternalId") String executionExternalId,
            @Param("organizationId") UUID organizationId);

    /**
     * Find all events for an external execution within an organization,
     * ordered oldest first so the execution timeline is chronological.
     */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.organizationId = :organizationId
              AND e.executionExternalId = :executionExternalId
            ORDER BY e.createdAt ASC
            """)
    List<ExecutionEvent> findByOrganizationIdAndExecutionExternalIdOrderByCreatedAtAsc(
            @Param("organizationId") UUID organizationId,
            @Param("executionExternalId") String executionExternalId);

    /**
     * Find all events for a specific external workflow execution within an
     * organization.
     *
     * <p>The workflow external ID is included to avoid accidentally mixing
     * executions from different external workflows that happen to reuse an
     * execution ID.
     */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.organizationId = :organizationId
              AND e.workflowExternalId = :workflowExternalId
              AND e.executionExternalId = :executionExternalId
            ORDER BY e.createdAt ASC
            """)
    List<ExecutionEvent> findByOrganizationIdAndWorkflowExternalIdAndExecutionExternalIdOrderByCreatedAtAsc(
            @Param("organizationId") UUID organizationId,
            @Param("workflowExternalId") String workflowExternalId,
            @Param("executionExternalId") String executionExternalId);

    /** List events for a workflow (by internal workflow ID), newest first. */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.workflowId = :workflowId
            ORDER BY e.createdAt DESC
            """)
    List<ExecutionEvent> findByWorkflowIdOrderByCreatedAtDesc(
            @Param("workflowId") UUID workflowId,
            Pageable pageable);

    /**
     * List events for a workflow within an organization, newest first. The org is
     * part of the query so a caller filtering by a workflow id from another tenant
     * gets an empty list rather than somebody else's events (contract §2).
     */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.organizationId = :organizationId
              AND e.workflowId = :workflowId
            ORDER BY e.createdAt DESC
            """)
    List<ExecutionEvent> findByOrganizationIdAndWorkflowIdOrderByCreatedAtDesc(
            @Param("organizationId") UUID organizationId,
            @Param("workflowId") UUID workflowId,
            Pageable pageable);

    /** List events for an organization, newest first. */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.organizationId = :organizationId
            ORDER BY e.createdAt DESC
            """)
    List<ExecutionEvent> findByOrganizationIdOrderByCreatedAtDesc(
            @Param("organizationId") UUID organizationId,
            Pageable pageable);

    /** Count events for a workflow. */
    @Query("""
            SELECT COUNT(e)
            FROM ExecutionEvent e
            WHERE e.workflowId = :workflowId
            """)
    long countByWorkflowId(
            @Param("workflowId") UUID workflowId);

    /** Count events by status for a workflow. */
    @Query("""
            SELECT COUNT(e)
            FROM ExecutionEvent e
            WHERE e.workflowId = :workflowId
              AND e.status = :status
            """)
    long countByWorkflowIdAndStatus(
            @Param("workflowId") UUID workflowId,
            @Param("status") String status);

    /**
     * Find an event by the composite idempotency identity.
     *
     * <p>Provider-synced rows always carry integrationId; legacy/custom rows
     * do not, so the null integrationId case (public inject API) is outside
     * this identity.
     */
    Optional<ExecutionEvent>
            findByIntegrationIdAndWorkflowExternalIdAndExecutionExternalIdAndStepExternalId(
                    @Param("integrationId") UUID integrationId,
                    @Param("workflowExternalId") String workflowExternalId,
                    @Param("executionExternalId") String executionExternalId,
                    @Param("stepExternalId") String stepExternalId);

    /** All events for an external workflow (provider), oldest first (first sync). */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.integrationId = :integrationId
              AND e.workflowExternalId = :workflowExternalId
            ORDER BY e.createdAt ASC
            """)
    List<ExecutionEvent> findByIntegrationIdAndProviderWorkflowId(
            @Param("integrationId") UUID integrationId,
            @Param("workflowExternalId") String workflowExternalId);

    /** Find events for an external workflow (provider) since a given time. */
    @Query("""
            SELECT e
            FROM ExecutionEvent e
            WHERE e.integrationId = :integrationId
              AND e.workflowExternalId = :workflowExternalId
              AND e.createdAt >= :since
            ORDER BY e.createdAt ASC
            """)
    List<ExecutionEvent> findByIntegrationIdAndProviderWorkflowIdAndCreatedAtAfter(
            @Param("integrationId") UUID integrationId,
            @Param("workflowExternalId") String workflowExternalId,
            @Param("since") Instant since);
}