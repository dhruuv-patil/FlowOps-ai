package com.flowops.reliability.detect;

import com.flowops.domain.AnomalyType;
import java.util.Map;

/**
 * A candidate anomaly emitted by a detector, before dedup/cooldown and persistence
 * (reliability pivot, M3).
 *
 * <p>A detector's only job is to compare one fresh observation against a learned
 * baseline and, if it deviates, describe <em>what</em> deviated in explainable terms.
 * Severity banding, dedup aggregation, and confidence gating are the recorder's job, so
 * a {@code Finding} is intentionally free of lifecycle state.
 *
 * @param type          the anomaly family
 * @param nodeId        the node it concerns, or {@code null} for a workflow-scoped finding
 * @param metric        the metric key (e.g. {@code LATENCY}), for display/grouping
 * @param expectedValue human-readable normal (e.g. {@code "p50 ≈ 1.2s (±0.3s)"})
 * @param actualValue   human-readable observation (e.g. {@code "9.8s"})
 * @param deviation     normalized magnitude in robust-sigma-equivalent units — the
 *                      common scale the severity model reads (see {@link SeverityCalculator})
 * @param confidence    0..1 trust in the signal, from sample size and signal strength
 * @param evidence      structured, secret-free rationale merged into the anomaly record
 * @param dedupKey      stable key that folds repeats of this exact problem together
 */
public record Finding(
        AnomalyType type,
        String nodeId,
        String metric,
        String expectedValue,
        String actualValue,
        double deviation,
        double confidence,
        Map<String, Object> evidence,
        String dedupKey) {
}
