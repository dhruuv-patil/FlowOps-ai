package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.AnomalyType;
import com.flowops.domain.MetricBaseline;
import com.flowops.domain.NodeExecutionMetric;
import com.flowops.domain.NodeRunStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Flags a workflow that "succeeded but walked a different path than normal" (reliability
 * pivot, M3).
 *
 * <p>Uses the workflow-scoped {@code BEHAVIOR} baseline, which learns each node's run
 * rate — the fraction of executions in which that node SUCCEEDED. A node with a run rate
 * near 1.0 is a backbone: it essentially always fires. If one run completed without that
 * node succeeding (it was skipped because its branch was not taken, or it failed), the
 * workflow behaved differently from its learned normal — e.g. the Slack notification node
 * that used to fire every run didn't. The run did not fail, so this is exactly the kind of
 * silent behavioral drift only a baseline can catch.
 *
 * <p>Only near-always nodes ({@code runRate >= 0.9}) are considered so optional branches
 * (genuinely conditional nodes) never false-positive. A finding is emitted per missing
 * node so each has its own dedup key.
 */
@Component
public class BehavioralDetector {

    private final ReliabilityProperties properties;

    public BehavioralDetector(ReliabilityProperties properties) {
        this.properties = properties;
    }

    /** Only nodes that succeed in at least this fraction of executions are "expected". */
    private static final double BACKBONE_RATE = 0.9;

    /**
     * @param baseline    the workflow's {@code BEHAVIOR} baseline (may be null/warming up)
     * @param executedRows telemetry for the just-completed execution
     * @return one finding per backbone node that did not succeed this run
     */
    public List<Finding> detect(MetricBaseline baseline, List<NodeExecutionMetric> executedRows) {
        List<Finding> findings = new ArrayList<>();
        // Warm-up / never-learned: nothing to compare against yet.
        if (!DetectorSupport.usable(baseline, properties.minSampleCount())
                || baseline.getSignature() == null) {
            return findings;
        }
        Object runRatesObj = baseline.getSignature().get("runRates");
        if (!(runRatesObj instanceof Map<?, ?> runRates)) {
            return findings;
        }

        Map<String, Boolean> succeeded = new LinkedHashMap<>();
        for (NodeExecutionMetric m : executedRows) {
            succeeded.put(m.getNodeId(), m.getStatus() == NodeRunStatus.SUCCEEDED);
        }

        double sampleFactor = DetectorSupport.clamp01(
                baseline.getSampleCount() / (2.0 * properties.minSampleCount()));

        for (Map.Entry<?, ?> e : runRates.entrySet()) {
            String expectedNode = String.valueOf(e.getKey());
            double runRate = e.getValue() instanceof Number n ? n.doubleValue() : 0.0;
            if (runRate < BACKBONE_RATE) {
                continue;
            }
            Boolean didRun = succeeded.get(expectedNode);
            if (Boolean.TRUE.equals(didRun)) {
                continue;
            }
            // Backbone node absent this run — the workflow took a different path.
            boolean statusKnown = succeeded.containsKey(expectedNode);
            double deviation = 4.0 + (statusKnown ? 1.0 : 0.0); // failed≠skipped: slightly worse
            double backboneFactor = DetectorSupport.clamp01(runRate);
            double confidence = DetectorSupport.round(sampleFactor * backboneFactor, 2);

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("kind", "backbone-node-missing");
            evidence.put("node", expectedNode);
            evidence.put("normalRunRate", DetectorSupport.round(runRate, 2));
            evidence.put("stateThisRun", statusKnown ? "did-not-succeed" : "absent-from-graph");
            evidence.put("sampleCount", baseline.getSampleCount());

            findings.add(new Finding(
                    AnomalyType.BEHAVIORAL,
                    expectedNode,
                    "BEHAVIOR",
                    "node \"" + expectedNode + "\" runs in "
                            + Math.round(runRate * 100) + "% of executions",
                    "it did not run this execution",
                    DetectorSupport.round(deviation, 2),
                    confidence,
                    evidence,
                    "BEHAVIORAL:" + expectedNode));
        }
        return findings;
    }
}
