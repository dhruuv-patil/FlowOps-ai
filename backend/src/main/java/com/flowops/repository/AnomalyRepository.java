package com.flowops.repository;

import com.flowops.domain.Anomaly;
import com.flowops.domain.AnomalyStatus;
import com.flowops.domain.AnomalyType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Anomaly read/write for the reliability layer. Every caller-facing finder is scoped by
 * {@code organizationId} (or a {@code workflowId}, unique within an org) so one tenant
 * can never read another's anomalies — a cross-tenant id simply resolves to empty.
 */
public interface AnomalyRepository extends JpaRepository<Anomaly, UUID> {

    /** Dedup/cooldown lookup: the most recent anomaly for a (workflow, dedupKey) pair. */
    Optional<Anomaly> findFirstByWorkflowIdAndDedupKeyOrderByDetectedAtDesc(
            UUID workflowId, String dedupKey);

    /** Org-scoped fetch by id (never a bare findById for a caller). */
    Optional<Anomaly> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Anomaly> findByOrganizationIdOrderByDetectedAtDesc(UUID organizationId);

    List<Anomaly> findByOrganizationIdAndStatusOrderByDetectedAtDesc(
            UUID organizationId, AnomalyStatus status);

    List<Anomaly> findByOrganizationIdAndWorkflowIdOrderByDetectedAtDesc(
            UUID organizationId, UUID workflowId);

    List<Anomaly> findByOrganizationIdAndTypeOrderByDetectedAtDesc(
            UUID organizationId, AnomalyType type);

    /** Open anomalies for a workflow (reliability-score inputs). */
    List<Anomaly> findByWorkflowIdAndStatus(UUID workflowId, AnomalyStatus status);

    /** Org-scoped variant: a workflowId from another tenant yields empty. */
    List<Anomaly> findByOrganizationIdAndWorkflowIdAndStatus(
            UUID organizationId, UUID workflowId, AnomalyStatus status);

    long countByOrganizationIdAndStatus(UUID organizationId, AnomalyStatus status);
}
