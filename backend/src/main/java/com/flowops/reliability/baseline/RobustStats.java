package com.flowops.reliability.baseline;

import java.util.Arrays;

/**
 * Robust summary statistics for the baseline engine.
 *
 * <p>Median + MAD (median absolute deviation) are used instead of mean/stddev for the
 * latency/output-size thresholds because they are resistant to outliers — a single 10x
 * slow run must not inflate the "normal" range and mask the anomaly it is an outlier
 * of. Percentiles use the nearest-rank method, which is deterministic and cheap.
 *
 * <p>{@link #of} returns {@code null} for an empty input, so callers can skip writing
 * a baseline when there are no usable samples.
 */
public final class RobustStats {

    /** Immutable summary of a numeric sample. */
    public record Summary(
            double mean, double median, double stddev, double mad,
            double p50, double p95, double p99, double min, double max, int n) {
    }

    private RobustStats() {
    }

    /**
     * @param values raw observations (unsorted). Empty/null-safe.
     */
    public static Summary of(double[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;

        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        double mean = sum / n;

        double sq = 0;
        for (double v : values) {
            double d = v - mean;
            sq += d * d;
        }
        double stddev = Math.sqrt(sq / n);

        double median = percentile(sorted, 50);
        double mad = medianAbsDeviation(sorted, median);

        return new Summary(
                mean,
                median,
                stddev,
                mad,
                percentile(sorted, 50),
                percentile(sorted, 95),
                percentile(sorted, 99),
                sorted[0],
                sorted[n - 1],
                n);
    }

    private static double medianAbsDeviation(double[] sortedAsc, double median) {
        double[] deviations = new double[sortedAsc.length];
        for (int i = 0; i < sortedAsc.length; i++) {
            deviations[i] = Math.abs(sortedAsc[i] - median);
        }
        double[] devSorted = deviations.clone();
        Arrays.sort(devSorted);
        return devSorted[devSorted.length / 2];
    }

    /** Nearest-rank percentile: deterministic, no interpolation. */
    private static double percentile(double[] sortedAsc, double pct) {
        if (sortedAsc.length == 0) {
            return 0;
        }
        int rank = (int) Math.ceil((pct / 100.0) * sortedAsc.length);
        if (rank < 1) {
            rank = 1;
        }
        if (rank > sortedAsc.length) {
            rank = sortedAsc.length;
        }
        return sortedAsc[rank - 1];
    }
}