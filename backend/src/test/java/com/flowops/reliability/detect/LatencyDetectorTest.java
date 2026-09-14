package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.MetricBaseline;
import com.flowops.domain.NodeExecutionMetric;
import com.flowops.domain.NodeRunStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the latency detector — pure, no Spring context, no DB (reliability
 * pivot, M3).
 */
class LatencyDetectorTest {

    private final ReliabilityProperties props = new ReliabilityProperties(
            java.time.Duration.ofHours(24), java.time.Duration.ofMinutes(5),
            30, java.time.Duration.ofHours(1),
            2.5, 3.0, 1.5, java.time.Duration.ofMinutes(15),
            0.15, 0.2, java.time.Duration.ofMinutes(30));
    private final LatencyDetector detector = new LatencyDetector(props);

    @Test
    void noFindingWhenRunDidNotSucceed() {
        NodeExecutionMetric m = obs(NodeRunStatus.FAILED, 5000L);
        assertThat(detector.detect(baseline(), m)).isEmpty();
    }

    @Test
    void noFindingWhenNoDuration() {
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED, null);
        assertThat(detector.detect(baseline(), m)).isEmpty();
    }

    @Test
    void noFindingDuringWarmup() {
        // sampleCount = 10 < minSampleCount(30) → warmup
        MetricBaseline b = baseline(10);
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED, 5000L);
        assertThat(detector.detect(b, m)).isEmpty();
    }

    @Test
    void noFindingWhenWithinNormal() {
        MetricBaseline b = baseline(50);
        // median=1000ms, MAD≈100ms → σ≈148ms → threshold = 1000 + 2.5*148 ≈ 1370
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED, 1300L);
        assertThat(detector.detect(b, m)).isEmpty();
    }

    @Test
    void findingWhenSpikePastThreshold() {
        MetricBaseline b = baseline(50);
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED, 2000L);
        var f = detector.detect(b, m);
        assertThat(f).isPresent();
        assertThat(f.get().type()).isEqualTo(com.flowops.domain.AnomalyType.LATENCY);
        assertThat(f.get().deviation()).isGreaterThan(0);
        assertThat(f.get().confidence()).isBetween(0.0, 1.0);
        assertThat(f.get().dedupKey()).isEqualTo("LATENCY:" + m.getNodeId());
    }

    @Test
    void confidenceRisesWithSampleCountAndSignal() {
        MetricBaseline bSmall = baseline(30); // just past warmup
        MetricBaseline bLarge = baseline(200); // well-trained
        NodeExecutionMetric m = obs(NodeRunStatus.SUCCEEDED, 3000L); // big spike
        var fSmall = detector.detect(bSmall, m).get();
        var fLarge = detector.detect(bLarge, m).get();
        assertThat(fLarge.confidence()).isGreaterThan(fSmall.confidence());
    }

    // ---- test helpers -------------------------------------------------------

    private MetricBaseline baseline(int sampleCount) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "test-node", "LATENCY");
        // Simulate normal latency around 1000ms with small spread
        b.setNumeric(new com.flowops.reliability.baseline.RobustStats.Summary(
                1000, 1000, 100, 100, 1000, 1200, 1400, 800, 1500, sampleCount));
        return b;
    }

    private MetricBaseline baseline() {
        return baseline(50);
    }

    private NodeExecutionMetric obs(NodeRunStatus status, Long durationMs) {
        NodeExecutionMetric m = NodeExecutionMetric.create(
                UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(),
                "test-node", "ai-agent", status);
        m.recordTiming(java.time.Instant.now().minusSeconds(10), java.time.Instant.now(), durationMs);
        return m;
    }
}