package com.flowops.reliability.detect;

import com.flowops.config.ReliabilityProperties;
import com.flowops.domain.AnomalyType;
import com.flowops.domain.MetricBaseline;
import com.flowops.domain.NodeExecutionMetric;
import com.flowops.domain.NodeRunStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Flags a node run that took abnormally long versus its learned latency (reliability
 * pivot, M3).
 *
 * <p>Compares the run's {@code durationMs} against the node's {@code LATENCY} baseline
 * using a robust threshold {@code median + sensitivity·σ} (σ from MAD — outlier
 * resistant). Only the slow side is an anomaly; a run that is faster than normal is not a
 * reliability problem. Suppressed entirely until the baseline clears warm-up
 * ({@code minSampleCount}).
 */
@Component
public class LatencyDetector {

    private final ReliabilityProperties properties;

    public LatencyDetector(ReliabilityProperties properties) {
        this.properties = properties;
    }

    /**
     * @param baseline the node's {@code LATENCY} baseline (may be null / warming up)
     * @param obs      the just-observed node run
     * @return a latency finding, or empty when normal, warming up, or not applicable
     */
    public Optional<Finding> detect(MetricBaseline baseline, NodeExecutionMetric obs) {
        if (obs.getStatus() != NodeRunStatus.SUCCEEDED || obs.getDurationMs() == null) {
            return Optional.empty();
        }
        if (!DetectorSupport.usable(baseline, properties.minSampleCount())) {
            return Optional.empty();
        }
        double median = baseline.getMedian() == null ? 0.0 : baseline.getMedian();
        double sigma = DetectorSupport.robustSigma(baseline);
        double actual = obs.getDurationMs();
        double threshold = median + properties.sensitivity() * sigma;

        if (actual <= threshold) {
            return Optional.empty();
        }

        double deviation = (actual - median) / sigma; // robust-sigma units
        double confidence = confidence(baseline.getSampleCount(), deviation);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("kind", "latency-spike");
        evidence.put("medianMs", DetectorSupport.round(median, 1));
        evidence.put("sigmaMs", DetectorSupport.round(sigma, 1));
        evidence.put("thresholdMs", DetectorSupport.round(threshold, 1));
        evidence.put("actualMs", actual);
        evidence.put("sigmaAway", DetectorSupport.round(deviation, 2));
        evidence.put("sampleCount", baseline.getSampleCount());

        String expected = "≈ " + DetectorSupport.duration(median)
                + " (normal up to " + DetectorSupport.duration(threshold) + ")";
        String actualText = DetectorSupport.duration(actual);

        return Optional.of(new Finding(
                AnomalyType.LATENCY,
                obs.getNodeId(),
                "LATENCY",
                expected,
                actualText,
                DetectorSupport.round(deviation, 2),
                confidence,
                evidence,
                "LATENCY:" + obs.getNodeId()));
    }

    /**
     * Confidence rises with sample size (a baseline built from more runs is more
     * trustworthy) and with how far past the threshold the run landed.
     */
    private double confidence(int sampleCount, double deviation) {
        double sampleFactor = DetectorSupport.clamp01(sampleCount / (2.0 * properties.minSampleCount()));
        double signalFactor = DetectorSupport.clamp01(deviation / (2.0 * properties.sensitivity()));
        return DetectorSupport.round(sampleFactor * signalFactor, 2);
    }
}
