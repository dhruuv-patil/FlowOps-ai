package com.flowops.reliability.detect;

import com.flowops.domain.AnomalySeverity;
import org.springframework.stereotype.Component;

/**
 * Turns a detector's normalized {@code deviation} + {@code confidence} + breadth of
 * impact into an explainable {@link AnomalySeverity} band (reliability pivot, M3).
 *
 * <p>The formula is deterministic and documented so a business user can see why an
 * anomaly is CRITICAL rather than LOW:
 *
 * <pre>
 *   score = deviation × confidence × impactFactor(affectedExecutions)
 *
 *   impactFactor = 1.50  when affectedExecutions ≥ 10   (widespread)
 *                = 1.25  when affectedExecutions ≥ 3    (recurring)
 *                = 1.00  otherwise                      (one-off)
 *
 *   score ≥ 8 → CRITICAL     ≥ 5 → HIGH     ≥ 3 → MEDIUM     else → LOW
 * </pre>
 *
 * <p>{@code deviation} is expressed by every detector on a shared robust-sigma-equivalent
 * scale (how many MADs / robust-z / rate-thresholds past normal), so one table of
 * thresholds bands all four anomaly families consistently. Multiplying by
 * {@code confidence} means a weakly-supported signal cannot reach a high band on its own;
 * the impact factor escalates a problem that keeps recurring across runs.
 */
@Component
public class SeverityCalculator {

    private static final double CRITICAL_SCORE = 8.0;
    private static final double HIGH_SCORE = 5.0;
    private static final double MEDIUM_SCORE = 3.0;

    private static final int WIDESPREAD_AFFECTED = 10;
    private static final int RECURRING_AFFECTED = 3;

    /** @return the banded severity for the given normalized signal. */
    public AnomalySeverity severity(double deviation, double confidence, int affectedExecutions) {
        double score = Math.abs(deviation) * clampConfidence(confidence) * impactFactor(affectedExecutions);
        if (score >= CRITICAL_SCORE) {
            return AnomalySeverity.CRITICAL;
        }
        if (score >= HIGH_SCORE) {
            return AnomalySeverity.HIGH;
        }
        if (score >= MEDIUM_SCORE) {
            return AnomalySeverity.MEDIUM;
        }
        return AnomalySeverity.LOW;
    }

    /** The composite score behind the band — recorded in evidence for transparency. */
    public double score(double deviation, double confidence, int affectedExecutions) {
        return Math.abs(deviation) * clampConfidence(confidence) * impactFactor(affectedExecutions);
    }

    private double impactFactor(int affectedExecutions) {
        if (affectedExecutions >= WIDESPREAD_AFFECTED) {
            return 1.5;
        }
        if (affectedExecutions >= RECURRING_AFFECTED) {
            return 1.25;
        }
        return 1.0;
    }

    private double clampConfidence(double confidence) {
        if (confidence < 0) {
            return 0;
        }
        return Math.min(confidence, 1.0);
    }
}
