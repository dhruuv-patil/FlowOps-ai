package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.MetricBaseline;
import com.flowops.domain.NodeExecutionMetric;
import com.flowops.domain.NodeRunStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the output detector — pure, no Spring context, no DB (reliability
 * pivot, M3). Tests structural drift: missing fields, type changes, null-rate spikes,
 * unexpected fields, and whole-output regression.
 */
class OutputDetectorTest {

    private final ReliabilityProperties props = new ReliabilityProperties(
            java.time.Duration.ofHours(24), java.time.Duration.ofMinutes(5),
            30, java.time.Duration.ofHours(1),
            2.5, 3.0, 1.5, java.time.Duration.ofMinutes(15),
            0.15, 0.2, java.time.Duration.ofMinutes(30));
    private final OutputDetector detector = new OutputDetector(props);

    @Test
    void noFindingWhenRunFailed() {
        NodeExecutionMetric m = obs(NodeRunStatus.FAILED);
        assertThat(detector.detect(schemaBaseline(50), sizeBaseline(50), m)).isEmpty();
    }

    @Test
    void noFindingDuringWarmup() {
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(observedSignature());
        assertThat(detector.detect(schemaBaseline(10), sizeBaseline(10), m)).isEmpty();
    }

    @Test
    void missingRequiredFieldIsFlagged() {
        MetricBaseline b = schemaBaseline(50);
        // Baseline: fields "id" and "status" always present (presentRate=1.0)
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(observedSignature(Map.of("id", field("string", false)))); // "status" missing
        var findings = detector.detect(b, sizeBaseline(50), m);
        // Finds: "status" missing, plus "unexpected field" if there were extra
        assertThat(findings).hasSizeGreaterThanOrEqualTo(1);
        boolean hasMissing = findings.stream().anyMatch(f -> f.expectedValue().contains("always present")
                && f.actualValue().contains("missing"));
        assertThat(hasMissing).isTrue();
    }

    @Test
    void typeChangeIsFlagged() {
        MetricBaseline b = schemaBaseline(50);
        // Baseline: "id" is string; observed makes it number
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(observedSignature(Map.of("id", field("number", false), "status", field("string", false))));
        var findings = detector.detect(b, sizeBaseline(50), m);
        assertThat(findings).hasSizeGreaterThanOrEqualTo(1);
        boolean hasTypeChange = findings.stream().anyMatch(f -> f.actualValue().contains("became"));
        assertThat(hasTypeChange).isTrue();
    }

    @Test
    void nullSpikeIsFlaggedWhenBaselineRarelyNull() {
        // Baseline has "token" with nullRate=0.01 (< 0.15 threshold)
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "test-node", "OUTPUT_SCHEMA");
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("rowCount", 50);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("token", Map.of("presentRate", 1.0, "nullRate", 0.01, "types", Map.of("string", 49)));
        sig.put("fields", fields);
        b.setSignature(sig);
        b.setSampleCount(50);

        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(observedSignature(Map.of("token", field("string", true)))); // "token" is null this run
        var findings = detector.detect(b, sizeBaseline(50), m);
        System.out.println("Findings for null spike test: " + findings);
        assertThat(findings).hasSizeGreaterThanOrEqualTo(1);
        boolean hasNullSpike = findings.stream().anyMatch(f -> f.actualValue().contains("null this run"));
        assertThat(hasNullSpike).isTrue();
    }

    
    @Test
    void nullNotFlaggedWhenBaselineOftenNull() {
        MetricBaseline b = schemaBaselineWithNullRate(50, 0.5); // baseline nullRate=50%
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(observedSignature(Map.of("optional", field("string", true))));
        var findings = detector.detect(b, sizeBaseline(50), m);
        assertThat(findings).isEmpty(); // 0.5 ≥ 0.15 threshold → no spike
    }

    @Test
    void unexpectedFieldsAreFlagged() {
        // Baseline has "id" and "status"; observed adds "newField"
        MetricBaseline b = schemaBaseline(50);
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        Map<String, Object> obsSig = observedSignature(Map.of(
                "id", field("string", false),
                "status", field("string", false),
                "newField", field("number", false)));
        m.recordSignature(obsSig);
        var findings = detector.detect(b, sizeBaseline(50), m);
        assertThat(findings).hasSizeGreaterThanOrEqualTo(1);
        boolean hasUnexpected = findings.stream().anyMatch(f ->
            f.actualValue().contains("not seen in baseline") || f.evidence().get("kind").equals("field-unexpected"));
        assertThat(hasUnexpected).isTrue();
    }

    @Test
    void wholeOutputNullIsFlagged() {
        MetricBaseline b = schemaBaseline(50); // expected object with 2 fields
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(nullSignature()); // NULL
        var findings = detector.detect(b, sizeBaseline(50), m);
        assertThat(findings).hasSizeGreaterThanOrEqualTo(1);
        boolean hasNull = findings.stream().anyMatch(f -> f.actualValue().contains("NULL"));
        assertThat(hasNull).isTrue();
    }

    @Test
    void outputSizeSpikeIsFlagged() {
        MetricBaseline sb = schemaBaseline(50);
        MetricBaseline sizeB = sizeBaseline(50); // median=10KB
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED);
        m.recordSignature(observedSignature());
        m.recordSizes(500, 100_000); // 100KB output vs 10KB median
        var findings = detector.detect(sb, sizeB, m);
        // schema findings + size finding
        assertThat(findings.size()).isGreaterThanOrEqualTo(1);
        boolean hasSize = findings.stream().anyMatch(f -> f.evidence().containsKey("kind")
                && "output-size-shift".equals(f.evidence().get("kind")));
        assertThat(hasSize).isTrue();
    }

    // ---- test helpers -------------------------------------------------------

    private MetricBaseline schemaBaseline(int sampleCount) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "test-node", "OUTPUT_SCHEMA");
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("rowCount", sampleCount);
        Map<String, Object> fields = new LinkedHashMap<>();
        // Two baseline fields: "id" (string, never null) and "status" (string, never null)
        fields.put("id", Map.of("presentRate", 1.0, "nullRate", 0.0, "types", Map.of("string", sampleCount)));
        fields.put("status", Map.of("presentRate", 1.0, "nullRate", 0.0, "types", Map.of("string", sampleCount)));
        sig.put("fields", fields);
        b.setSignature(sig);
        b.setSampleCount(sampleCount);
        return b;
    }

    private MetricBaseline schemaBaselineWithNullRate(int sampleCount, double nullRate) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "test-node", "OUTPUT_SCHEMA");
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("rowCount", sampleCount);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("optional", Map.of("presentRate", 1.0, "nullRate", nullRate,
                "types", Map.of("string", (int)(sampleCount * (1 - nullRate)))));
        sig.put("fields", fields);
        b.setSignature(sig);
        b.setSampleCount(sampleCount);
        return b;
    }

    private MetricBaseline sizeBaseline(int sampleCount) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "test-node", "OUTPUT_SIZE");
        // median 10KB
        b.setNumeric(new com.flowops.reliability.baseline.RobustStats.Summary(
                10000, 10000, 1000, 1000, 10000, 12000, 14000, 8000, 15000, sampleCount));
        return b;
    }

    private NodeExecutionMetric obs(NodeRunStatus status) {
        NodeExecutionMetric m = NodeExecutionMetric.create(
                UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(),
                "test-node", "ai-agent", status);
        return m;
    }

    private Map<String, Object> observedSignature(Map<String, Object> fields) {
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("form", "OBJECT");
        sig.put("fieldCount", fields.size());
        sig.put("fields", fields);
        return sig;
    }

    private Map<String, Object> observedSignature() {
        return observedSignature(Map.of(
                "id", field("string", false),
                "status", field("string", false)));
    }

    private Map<String, Object> nullSignature() {
        Map<String, Object> sig = new LinkedHashMap<>();
        sig.put("form", "NULL");
        return sig;
    }

    private Map<String, Object> field(String type, boolean isNull) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("type", type);
        f.put("null", isNull);
        return f;
    }
}