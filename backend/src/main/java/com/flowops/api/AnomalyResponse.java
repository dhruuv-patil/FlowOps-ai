package com.flowops.api;

import com.flowops.domain.Anomaly;
import com.flowops.domain.AnomalySeverity;
import com.flowops.domain.AnomalyStatus;
import com.flowops.domain.AnomalyType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Wire representation of an anomaly (reliability pivot, M4).
 *
 * <p>Everything here is already secret-free in the entity: sizes, structure,
 * aggregates, and the explainable evidence — never payload values.
 */
public record AnomalyResponse(
        UUID id,
        UUID workflowId,
        String nodeId,
        UUID executionId,
        AnomalyType type,
        AnomalySeverity severity,
        AnomalyStatus status,
        String metric,
        String expectedValue,
        String actualValue,
        Double deviation,
        Double confidence,
        int affectedExecutions,
        Map<String, Object> evidence,
        Instant detectedAt,
        Instant updatedAt) {

    public static AnomalyResponse from(Anomaly a) {
        return new AnomalyResponse(
                a.getId(),
                a.getWorkflowId(),
                a.getNodeId(),
                a.getExecutionId(),
                a.getType(),
                a.getSeverity(),
                a.getStatus(),
                a.getMetric(),
                a.getExpectedValue(),
                a.getActualValue(),
                a.getDeviation(),
                a.getConfidence(),
                a.getAffectedExecutions(),
                a.getEvidence(),
                a.getDetectedAt(),
                a.getUpdatedAt());
    }
}
