package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.MetricBaseline;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the volume detector — pure, no Spring context, no DB (reliability
 * pivot, M3). Tests surge and drop detection using Poisson-consistent sqrt(mean) scaling.
 */
class VolumeDetectorTest {

    private final ReliabilityProperties props = new ReliabilityProperties(
            java.time.Duration.ofHours(24), java.time.Duration.ofMinutes(5),
            30, java.time.Duration.ofHours(1),
            2.5, 3.0, 1.5, java.time.Duration.ofMinutes(15),
            0.15, 0.2, java.time.Duration.ofMinutes(30));
    // Need volume-drop-window; use reflection or add to props for test
    private final VolumeDetector detector = new VolumeDetector(props);

    @Test
    void noFindingDuringWarmup() {
        MetricBaseline b = volumeBaseline(10, 100); // sampleCount < 30
        assertThat(detector.detect(b, 10, 100)).isEmpty();
    }

    @Test
    void noFindingWhenNullBaseline() {
        assertThat(detector.detect(null, 10, 100)).isEmpty();
    }

    @Test
    void noFindingWhenMedianZero() {
        MetricBaseline b = volumeBaseline(50, 0);
        assertThat(detector.detect(b, 10, 100)).isEmpty();
    }

    @Test
    void surgeDetection() {
        // 24h / 5m = 288 windows; median 100 per 24h → expectedPerWin = 100/288 ≈ 0.35, floored to 1
        // z = (current - 1) / sqrt(1) = current - 1; threshold 3.0
        // For surge we need current ≥ 4 AND ratio ≥ 1.5 (current ≥ 1.5)
        MetricBaseline b = volumeBaseline(50, 100); // median=100 total per 24h
        // With expected≈1, threshold z≥3 means current≥4, ratio≥1.5 means current≥2
        // So current=4 triggers surge
        var f = detector.detect(b, 4, 50); // recent=50 (normal, not a drop)
        assertThat(f).isPresent();
        assertThat(f.get().type()).isEqualTo(com.flowops.domain.AnomalyType.VOLUME);
        assertThat(f.get().actualValue()).contains("4");
        assertThat(f.get().evidence().get("kind")).isEqualTo("volume-surge");
    }

    @Test
    void dropDetection() {
        // median=1000/24h, 288 windows → expectedPerWin=4, dropWindow (30m) = 6 windows
        // expectedDrop = 4*6=24; sigmaDrop=sqrt(24)≈4.9
        // zDrop threshold -3 → recent ≤ 24 - 3*4.9 ≈ 9.3; ratio floor 1.5 → recent ≤ 16
        MetricBaseline b = volumeBaseline(50, 1000);
        var f = detector.detect(b, 2, 5); // current window normal (2≈expected), recent window very low
        assertThat(f).isPresent();
        assertThat(f.get().evidence().get("kind")).isEqualTo("volume-drop");
        assertThat(f.get().actualValue()).contains("5");
    }

    @Test
    void normalVolumeNoFinding() {
        MetricBaseline b = volumeBaseline(50, 100);
        assertThat(detector.detect(b, 1, 40)).isEmpty(); // current=1 (within noise), recent=40 (normal)
    }

    private MetricBaseline volumeBaseline(int sampleCount, double median) {
        MetricBaseline b = MetricBaseline.create(UUID.randomUUID(), UUID.randomUUID(), "", "VOLUME");
        b.setNumeric(new com.flowops.reliability.baseline.RobustStats.Summary(
                median, median, Math.sqrt(median), Math.sqrt(median),
                median, median * 1.5, median * 2, 1, median * 3, sampleCount));
        return b;
    }
}