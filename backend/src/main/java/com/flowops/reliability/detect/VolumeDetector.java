package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.AnomalyType;
import com.flowops.domain.MetricBaseline;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Flags a workflow whose recent execution volume is off its learned distribution
 * (reliability pivot, M3).
 *
 * <p>The {@code VOLUME} baseline stores rolling execution counts summarized robustly
 * (median). This detector compares fresh counts against the per-window rate implied by
 * that median:
 *
 * <pre>
 *   windows      = baselineWindow / schedulerDelay
 *   expPerWin    = max(1, median / windows)
 *
 *   SURGE  (one scheduler window)  z = (current - expPerWin) / sqrt(expPerWin)
 *          flag if z ≥ volumeZThreshold AND current ≥ volumeRatioFloor × expPerWin
 *
 *   DROP   (volumeDropWindow, wider so a silent stop is not lost in Poisson noise)
 *          expDrop  = expPerWin × dropWindows
 *          z        = (recent - expDrop) / sqrt(expDrop)
 *          flag if z ≤ -volumeZThreshold AND recent ≤ expDrop / volumeRatioFloor
 * </pre>
 *
 * <p>Using the Poisson-consistent scale {@code sqrt(mean)} keeps the per-window gates
 * honest at low volumes (where a single quiet tick is common) while the ratio floor
 * guards against noisy z-scores on tiny expectations. Pure compute: the caller supplies
 * both counts so the detector stays unit-testable; warm-up is the baseline's sample count.
 */
@Component
public class VolumeDetector {

    private final ReliabilityProperties properties;

    public VolumeDetector(ReliabilityProperties properties) {
        this.properties = properties;
    }

    /**
     * @param baseline    the workflow-scoped {@code VOLUME} baseline (may be null/warming up)
     * @param currentCount distinct executions in the last scheduler window
     * @param recentCount  distinct executions in the wider {@code volumeDropWindow}
     * @return a volume finding, or empty when normal / warming up
     */
    public Optional<Finding> detect(MetricBaseline baseline, long currentCount, long recentCount) {
        if (!DetectorSupport.usable(baseline, properties.minSampleCount())) {
            return Optional.empty();
        }
        double median = baseline.getMedian() == null ? 0.0 : baseline.getMedian();
        if (median <= 0.0) {
            return Optional.empty();
        }
        long windows = windowsInBaselineWindow();
        if (windows < 1) {
            return Optional.empty();
        }
        double expectedPerWin = Math.max(1.0, median / windows);
        double sigmaPerWin = Math.max(1.0, Math.sqrt(expectedPerWin));
        double z = (currentCount - expectedPerWin) / sigmaPerWin;
        double ratio = currentCount / expectedPerWin;

        boolean surge = z >= properties.volumeZThreshold()
                && ratio >= properties.volumeRatioFloor();

        long dropWindows = dropWindows();
        double expectedDrop = expectedPerWin * Math.max(1, dropWindows);
        double sigmaDrop = Math.max(1.0, Math.sqrt(expectedDrop));
        double zDrop = (recentCount - expectedDrop) / sigmaDrop;
        boolean drop = dropWindows > 0
                && zDrop <= -properties.volumeZThreshold()
                && recentCount <= expectedDrop / properties.volumeRatioFloor();

        if (!surge && !drop) {
            return Optional.empty();
        }

        String kind = surge ? "volume-surge" : "volume-drop";
        double deviation = Math.abs(surge ? z : zDrop);
        double confidence = DetectorSupport.round(
                DetectorSupport.clamp01(baseline.getSampleCount() / (2.0 * properties.minSampleCount()))
                        * DetectorSupport.clamp01(deviation / (2.0 * properties.volumeZThreshold())),
                2);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("kind", kind);
        evidence.put("windowExecutions", currentCount);
        evidence.put("expectedPerWindow", DetectorSupport.round(expectedPerWin, 2));
        evidence.put("ratio", DetectorSupport.round(ratio, 2));
        evidence.put("zScore", DetectorSupport.round(z, 2));
        evidence.put("dropWindowExecutions", recentCount);
        evidence.put("expectedPerDropWindow", DetectorSupport.round(expectedDrop, 2));
        evidence.put("dropZScore", DetectorSupport.round(zDrop, 2));
        evidence.put("sampleCount", baseline.getSampleCount());

        String expected = surge
                ? "≤ " + Math.round(expectedPerWin * properties.volumeRatioFloor())
                        + " execution(s)/window (normal ≈ " + Math.round(expectedPerWin) + ")"
                : "≥ " + Math.round(expectedDrop / properties.volumeRatioFloor())
                        + " execution(s)/" + properties.volumeDropWindow().toMinutes() + "m"
                        + " (normal ≈ " + Math.round(expectedDrop) + " in that period)";
        String actual = surge
                ? currentCount + " execution(s) in the last window"
                : recentCount + " execution(s) in the last "
                        + properties.volumeDropWindow().toMinutes() + "m";

        return Optional.of(new Finding(
                AnomalyType.VOLUME,
                null, // workflow-scoped
                "VOLUME",
                expected,
                actual,
                DetectorSupport.round(deviation, 2),
                confidence,
                evidence,
                "VOLUME:" + baseline.getWorkflowId()));
    }

    private long windowsInBaselineWindow() {
        return ratioWindows(properties.baselineWindow().toMillis(), properties.schedulerDelay().toMillis());
    }

    private long dropWindows() {
        return ratioWindows(properties.volumeDropWindow().toMillis(), properties.schedulerDelay().toMillis());
    }

    private long ratioWindows(long totalMs, long stepMs) {
        if (totalMs <= 0 || stepMs <= 0) {
            return 0;
        }
        return Math.max(1, Math.round((double) totalMs / stepMs));
    }
}
