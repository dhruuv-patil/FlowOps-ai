package com.flowops.repository;

import com.flowops.domain.Workflow;
import com.flowops.domain.WorkflowStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * All reads are org-scoped by construction: there is no bare {@code findById}
 * exposed to callers, so a workflow can only ever be fetched together with its
 * owning organization id (contract §2 rule 6).
 */
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    Optional<Workflow> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Listing with an optional case-insensitive name search and optional status
     * filter. Nulls disable the respective clause. Archived rows are included
     * only when explicitly filtered to {@code ARCHIVED}.
     */
    @Query("""
            SELECT w FROM Workflow w
            WHERE w.organizationId = :organizationId
              AND (:search IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR w.status = :status)
              AND (:status IS NOT NULL OR w.status <> com.flowops.domain.WorkflowStatus.ARCHIVED)
            ORDER BY w.updatedAt DESC
            """)
    List<Workflow> search(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            @Param("status") WorkflowStatus status);
}
