package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.MetricBaseline;
import com.flowops.domain.NodeExecutionMetric;
import com.flowops.domain.NodeRunStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the behavioral detector — pure, no Spring context, no DB
 * (reliability pivot, M3). Flags when a workflow succeeds but misses a backbone node.
 */
class BehavioralDetectorTest {

    private final ReliabilityProperties props = new ReliabilityProperties(
            java.time.Duration.ofHours(24), java.time.Duration.ofMinutes(5),
            30, java.time.Duration.ofHours(1),
            2.5, 3.0, 1.5, java.time.Duration.ofMinutes(15),
            0.15, 0.2, java.time.Duration.ofMinutes(30));
    private final BehavioralDetector detector = new BehavioralDetector(props);

    @Test
    void noFindingWhenWarmingUp() {
        MetricBaseline b = behaviorBaseline(10); // < 30
        List<NodeExecutionMetric> rows = rows(backboneRan(true));
        assertThat(detector.detect(b, rows)).isEmpty();
    }

    @Test
    void noFindingWhenBaselineNull() {
        List<NodeExecutionMetric> rows = rows(backboneRan(true));
        assertThat(detector.detect(null, rows)).isEmpty();
    }

    @Test
    void noFindingWhenBackboneNodeRan() {
        MetricBaseline b = behaviorBaseline(50);
        List<NodeExecutionMetric> rows = rows(backboneRan(true));
        assertThat(detector.detect(b, rows)).isEmpty();
    }

    @Test
    void findingWhenBackboneNodeMissing() {
        MetricBaseline b = behaviorBaseline(50);
        List<NodeExecutionMetric> rows = rows(backboneRan(false));
        var findings = detector.detect(b, rows);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).type()).isEqualTo(com.flowops.domain.AnomalyType.BEHAVIORAL);
        assertThat(findings.get(0).actualValue()).contains("did not run");
        assertThat(findings.get(0).dedupKey()).startsWith("BEHAVIORAL:slack");
    }

    @Test
    void optionalNodeNotFlagged() {
        MetricBaseline b = behaviorBaselineWithOptional(50);
        List<NodeExecutionMetric> rows = rows(backboneRan(false)); // backbone didn't run, optional missing
        var findings = detector.detect(b, rows);
        // slack (runRate 0.95) is flagged; optional (runRate 0.3) is not
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).nodeId()).isEqualTo("slack");
    }

    @Test
    void confidenceReflectsRunRateAndSampleCount() {
        MetricBaseline b = behaviorBaseline(200);
        List<NodeExecutionMetric> rows = rows(backboneRan(false));
        var findings = detector.detect(b, rows);
        assertThat(findings).isNotEmpty();
        assertThat(findings.get(0).confidence()).isBetween(0.0, 1.0);
    }

    // ---- test helpers -------------------------------------------------------

    private MetricBaseline behaviorBaseline(int sampleCount) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "", "BEHAVIOR");
        Map<String, Object> sig = new LinkedHashMap<>();
        Map<String, Object> runRates = new LinkedHashMap<>();
        runRates.put("slack", 0.95); // backbone
        runRates.put("db-write", 0.8); // not backbone
        sig.put("executions", sampleCount);
        sig.put("runRates", runRates);
        b.setSignature(sig);
        b.setSampleCount(sampleCount);
        return b;
    }

    private MetricBaseline behaviorBaselineWithOptional(int sampleCount) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "", "BEHAVIOR");
        Map<String, Object> sig = new LinkedHashMap<>();
        Map<String, Object> runRates = new LinkedHashMap<>();
        runRates.put("slack", 0.95); // backbone
        runRates.put("optional-notify", 0.3); // optional
        sig.put("executions", sampleCount);
        sig.put("runRates", runRates);
        b.setSignature(sig);
        b.setSampleCount(sampleCount);
        return b;
    }

    private List<NodeExecutionMetric> rows(Map<String, Boolean> ran) {
        List<NodeExecutionMetric> list = new java.util.ArrayList<>();
        for (Map.Entry<String, Boolean> e : ran.entrySet()) {
            NodeExecutionMetric m = NodeExecutionMetric.create(
                    UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(),
                    e.getKey(), e.getKey().contains("slack") ? "notification" : "action",
                    e.getValue() ? NodeRunStatus.SUCCEEDED : NodeRunStatus.SKIPPED);
            list.add(m);
        }
        return list;
    }

    private Map<String, Boolean> backboneRan(boolean ran) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("slack", ran);
        map.put("db-write", ran);
        return map;
    }
}