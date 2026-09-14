package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.MetricBaseline;
import com.flowops.domain.NodeExecutionMetric;
import com.flowops.domain.NodeRunStatus;
import com.flowops.reliability.baseline.RobustStats;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Canonical test fixtures for the detector unit tests (all pure, no Spring). */
final class DetectorTestData {

    static final UUID ORG = UUID.randomUUID();
    static final UUID WORKFLOW = UUID.randomUUID();
    static final UUID EXECUTION = UUID.randomUUID();

    private DetectorTestData() {
    }

    static ReliabilityProperties props() {
        return new ReliabilityProperties(
                Duration.ofHours(24), // baseline window
                Duration.ofMinutes(5), // scheduler delay
                30,                    // min sample count
                Duration.ofHours(1),   // warm-up
                2.5,                   // sensitivity
                3.0,                   // volume z
                1.5,                   // volume ratio floor
                Duration.ofMinutes(15),// cooldown
                0.15,                  // null-rate spike
                0.2,                   // min confidence
                Duration.ofMinutes(30)); // volume drop window
    }

    /** A numeric baseline whose values are all {@code value} — median=value, MAD=0. */
    static MetricBaseline numericBaseline(String metric, String nodeId, int n, double value) {
        double[] arr = new double[n];
        java.util.Arrays.fill(arr, value);
        MetricBaseline b = MetricBaseline.create(ORG, WORKFLOW, nodeId, metric);
        b.setNumeric(RobustStats.of(arr));
        return b;
    }

    /** A baseline with an explicit median (via values \{median-Δ, median, median+Δ\}). */
    static MetricBaseline numericBaselineWithMedian(String metric, String nodeId, int n,
            double median, double spread) {
        double[] arr = new double[n];
        for (int i = 0; i < n; i++) {
            arr[i] = median + spread * (i % 3 - 1);
        }
        MetricBaseline b = MetricBaseline.create(ORG, WORKFLOW, nodeId, metric);
        b.setNumeric(RobustStats.of(arr));
        return b;
    }

    static MetricBaseline schemaBaseline(String nodeId, Map<String, Object> fields, int sampleCount) {
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("rowCount", sampleCount);
        sig.put("fields", fields);
        MetricBaseline b = MetricBaseline.create(ORG, WORKFLOW, nodeId, "OUTPUT_SCHEMA");
        b.setSignature(sig);
        b.setSampleCount(sampleCount);
        return b;
    }

    static MetricBaseline behaviorBaseline(Map<String, Object> runRates, int sampleCount) {
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("executions", sampleCount);
        sig.put("runRates", runRates);
        MetricBaseline b = MetricBaseline.create(ORG, WORKFLOW, "", "BEHAVIOR");
        b.setSignature(sig);
        b.setSampleCount(sampleCount);
        return b;
    }

    static NodeExecutionMetric nodeMetric(String nodeId, NodeRunStatus status) {
        return NodeExecutionMetric.create(ORG, WORKFLOW, 3, EXECUTION, nodeId, "test", status);
    }

    static NodeExecutionMetric succeededNode(String nodeId, long durationMs, Integer outputSize) {
        NodeExecutionMetric m = nodeMetric(nodeId, NodeRunStatus.SUCCEEDED);
        m.recordTiming(Instant.now().minusSeconds(5), Instant.now(), durationMs);
        if (outputSize != null) {
            m.recordSizes(10, outputSize);
        }
        return m;
    }

    /** {@code fields} maps field name -> {type, null}. */
    static Map<String, Object> outputSignature(Map<String, Map<String, Object>> fields) {
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("form", "OBJECT");
        sig.put("fieldCount", fields.size());
        Map<String, Object> f = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : fields.entrySet()) {
            f.put(e.getKey(), e.getValue());
        }
        sig.put("fields", f);
        return sig;
    }

    static Map<String, Object> nonNull(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("null", false);
        return m;
    }

    static Map<String, Object> isNull() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "null");
        m.put("null", true);
        return m;
    }

    /** Baseline {@code fields} map: name -> {presentRate, nullRate, types}. */
    static Map<String, Object> field(String type, double presentRate) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("presentRate", presentRate);
        m.put("nullRate", 0.0);
        Map<String, Integer> types = new LinkedHashMap<>();
        types.put(type, 30);
        m.put("types", types);
        return m;
    }

    static List<NodeExecutionMetric> rows(NodeExecutionMetric... metrics) {
        List<NodeExecutionMetric> l = new ArrayList<>();
        java.util.Collections.addAll(l, metrics);
        return l;
    }
}
