package com.flowops.api;

import java.util.List;
import java.util.UUID;

/**
 * Per-workflow reliability health (reliability pivot, M4).
 *
 * <p>The score is deterministic and documented (see
 * {@link com.flowops.reliability.ReliabilityService#score}): start at 100 and subtract
 * weighted penalties for the recent failure rate and for each open anomaly, scaled by the
 * {@code severity} factor and decayed by how old the anomaly is. {@code components} makes
 * the subtraction transparent — the user sees exactly why a workflow scored what it did.
 */
public record WorkflowHealthResponse(
        UUID workflowId,
        double score,
        double successRate,
        int executions,
        int openAnomalies,
        List<Component> components) {

    /** One documented penalty line, so the score is explainable. */
    public record Component(String label, double penalty, String detail) {
    }
}
