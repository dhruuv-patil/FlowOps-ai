package com.flowops.repository;

import com.flowops.domain.MetricBaseline;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Baseline upsert/read for the reliability layer. */
public interface MetricBaselineRepository extends JpaRepository<MetricBaseline, UUID> {

    /** The existing baseline for a (workflow, node, metric) key, if any. */
    Optional<MetricBaseline> findByWorkflowIdAndNodeIdAndMetric(
            UUID workflowId, String nodeId, String metric);

    /** Every baseline for a workflow (used by the reliability API). */
    java.util.List<MetricBaseline> findByWorkflowId(UUID workflowId);

    /** Baselines of one metric across workflows (volume sweep scans these). */
    java.util.List<MetricBaseline> findByMetric(String metric);

    /** All baselines for a workflow filtered by metric (any node). */
    java.util.List<MetricBaseline> findByWorkflowIdAndMetric(UUID workflowId, String metric);

    /** Org-scoped variant: workflowId from another tenant yields empty. */
    java.util.List<MetricBaseline> findByOrganizationIdAndWorkflowIdAndMetric(
            UUID organizationId, UUID workflowId, String metric);

    /** Org-scoped variant: workflowId from another tenant yields empty. */
    Optional<MetricBaseline> findByOrganizationIdAndWorkflowIdAndNodeIdAndMetric(
            UUID organizationId, UUID workflowId, String nodeId, String metric);
}