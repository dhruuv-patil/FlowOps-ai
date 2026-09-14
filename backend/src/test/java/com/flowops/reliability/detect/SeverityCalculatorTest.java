package com.flowops.reliability.detect;

import com.flowops.domain.AnomalySeverity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the explainable severity model (reliability pivot, M3).
 *
 * <p>Score = deviation × confidence × impactFactor
 * impactFactor = 1.5 (≥10 affected), 1.25 (≥3 affected), 1.0 (one-off)
 * LOW < 3, MEDIUM ≥ 3, HIGH ≥ 5, CRITICAL ≥ 8
 */
class SeverityCalculatorTest {

    private final SeverityCalculator calc = new SeverityCalculator();

    @Test
    void lowForTinySignalWithLowConfidenceAndOneOff() {
        // deviation=1 (1σ), confidence=0.2, single run → score = 0.2
        AnomalySeverity s = calc.severity(1.0, 0.2, 1);
        assertThat(s).isEqualTo(AnomalySeverity.LOW);
    }

    @Test
    void mediumWhenScoreReachesThree() {
        // deviation=3, confidence=1.0, single run → score = 3.0 → MEDIUM
        AnomalySeverity s = calc.severity(3.0, 1.0, 1);
        assertThat(s).isEqualTo(AnomalySeverity.MEDIUM);
    }

    @Test
    void highWhenScoreReachesFive() {
        // deviation=5, confidence=1.0, single run → score = 5.0 → HIGH
        AnomalySeverity s = calc.severity(5.0, 1.0, 1);
        assertThat(s).isEqualTo(AnomalySeverity.HIGH);
    }

    @Test
    void criticalWhenScoreReachesEight() {
        // deviation=8, confidence=1.0, single run → score = 8.0 → CRITICAL
        AnomalySeverity s = calc.severity(8.0, 1.0, 1);
        assertThat(s).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    void recurringBumpsOneOffFromLowToMedium() {
        // deviation=2.5, conf=1.0, single → 2.5 (LOW); 4 affected → ×1.25 = 3.125 (MEDIUM)
        AnomalySeverity s1 = calc.severity(2.5, 1.0, 1);
        AnomalySeverity s4 = calc.severity(2.5, 1.0, 4);
        assertThat(s1).isEqualTo(AnomalySeverity.LOW);
        assertThat(s4).isEqualTo(AnomalySeverity.MEDIUM);
    }

    @Test
    void recurringBumpsMediumToHigh() {
        // deviation=4, conf=1.0, single → 4.0 (MEDIUM); 4 affected → ×1.25 = 5.0 (HIGH)
        AnomalySeverity s1 = calc.severity(4.0, 1.0, 1);
        AnomalySeverity s4 = calc.severity(4.0, 1.0, 4);
        assertThat(s1).isEqualTo(AnomalySeverity.MEDIUM);
        assertThat(s4).isEqualTo(AnomalySeverity.HIGH);
    }

    @Test
    void widespreadEscalatesHighToCritical() {
        // deviation=5.5, conf=1.0, single → 5.5 (HIGH); 10 affected → ×1.5 = 8.25 (CRITICAL)
        AnomalySeverity s1 = calc.severity(5.5, 1.0, 1);
        AnomalySeverity s10 = calc.severity(5.5, 1.0, 10);
        assertThat(s1).isEqualTo(AnomalySeverity.HIGH);
        assertThat(s10).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    void confidenceClampedToZeroOne() {
        // negative confidence → treated as 0 → LOW
        AnomalySeverity s = calc.severity(10.0, -0.5, 1);
        assertThat(s).isEqualTo(AnomalySeverity.LOW);
        // >1 → clamped to 1
        AnomalySeverity s2 = calc.severity(10.0, 1.5, 1);
        assertThat(s2).isEqualTo(calc.severity(10.0, 1.0, 1));
    }

    @Test
    void scoreIsDeterministicAndExplainable() {
        double score = calc.score(4.0, 0.5, 1);
        assertThat(score).isEqualTo(2.0); // 4 * 0.5 * 1.0
        // recorded in evidence so business users see the math
    }
}