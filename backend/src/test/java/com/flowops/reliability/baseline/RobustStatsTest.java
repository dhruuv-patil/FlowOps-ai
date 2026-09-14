package com.flowops.reliability.baseline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RobustStats}. No Spring context.
 *
 * <p>The key property under test is robustness: median + MAD are used precisely so an
 * outlier does not drag the "normal" range toward the anomaly. These tests pin the
 * median/MAD/percentile calculations and the nearest-rank method the thresholds build on.
 */
class RobustStatsTest {

    @Test
    void medianIsCorrectForOddAndEvenCounts() {
        assertThat(RobustStats.of(new double[]{1, 2, 3, 4, 5}).median()).isEqualTo(3);
        assertThat(RobustStats.of(new double[]{1, 2, 3, 4}).median()).isEqualTo(2); // lower middle
    }

    @Test
    void singleOutlierDoesNotMoveTheMedian() {
        RobustStats.Summary withOutlier = RobustStats.of(new double[]{1, 2, 2, 2, 2, 2, 2, 2, 2, 100});
        assertThat(withOutlier.median()).isEqualTo(2);
        assertThat(withOutlier.mad()).isEqualTo(0);          // the bulk is identical
        assertThat(withOutlier.mean()).isEqualTo(11.7);      // 115 / 10
        assertThat(withOutlier.p95()).isGreaterThan(50);     // nearest-rank returns the outlier
    }

    @Test
    void percentilesUseNearestRank() {
        RobustStats.Summary s = RobustStats.of(new double[]{100, 200, 300});
        assertThat(s.p50()).isEqualTo(200);   // ceil(0.5*3)=2
        assertThat(s.p95()).isEqualTo(300);   // ceil(0.95*3)=3
        assertThat(s.p99()).isEqualTo(300);
        assertThat(s.min()).isEqualTo(100);
        assertThat(s.max()).isEqualTo(300);
    }

    @Test
    void emptyInputYieldsNull() {
        assertThat(RobustStats.of(new double[]{})).isNull();
        assertThat(RobustStats.of(null)).isNull();
    }

    @Test
    void singleValueHasZeroDispersion() {
        RobustStats.Summary s = RobustStats.of(new double[]{42});
        assertThat(s.median()).isEqualTo(42);
        assertThat(s.mad()).isEqualTo(0);
        assertThat(s.stddev()).isEqualTo(0);
        assertThat(s.n()).isEqualTo(1);
    }

    @Test
    void madMeasuresDispersionAroundMedian() {
        // Values: 5,6,7,8,9. Median=7. Deviations: 2,1,0,1,2 -> sorted: 0,1,1,2,2. Median=1.
        RobustStats.Summary s = RobustStats.of(new double[]{5, 6, 7, 8, 9});
        assertThat(s.median()).isEqualTo(7);
        assertThat(s.mad()).isEqualTo(1);
    }
}