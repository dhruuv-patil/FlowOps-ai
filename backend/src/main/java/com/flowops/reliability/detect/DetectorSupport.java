package com.flowops.reliability.detect;

import com.flowops.domain.MetricBaseline;

/**
 * Small, side-effect-free helpers shared by the detectors (reliability pivot, M3):
 * baseline usability (warm-up gate), robust dispersion, value formatting for the
 * human-readable {@code expected}/{@code actual} strings, and dedup-key bounding.
 */
public final class DetectorSupport {

    /** 1 / Φ⁻¹(3/4): scales MAD to a stddev-equivalent for a normal distribution. */
    public static final double MAD_TO_SIGMA = 1.4826;

    private DetectorSupport() {
    }

    /**
     * A baseline is usable only once it has at least {@code minSampleCount} observations.
     * Below that it is warm-up: detectors suppress anomalies rather than invent a normal
     * range from a handful of runs. A null baseline (never computed) is never usable.
     */
    public static boolean usable(MetricBaseline baseline, int minSampleCount) {
        return baseline != null && baseline.getSampleCount() >= minSampleCount;
    }

    /**
     * Robust dispersion in stddev-equivalent units. Uses MAD (outlier-resistant); when the
     * bulk of samples are identical (MAD 0) it falls back to a small floor derived from the
     * median so a single deviation does not read as an infinite z-score.
     */
    public static double robustSigma(MetricBaseline baseline) {
        double mad = baseline.getMad() == null ? 0.0 : baseline.getMad();
        double sigma = mad * MAD_TO_SIGMA;
        if (sigma > 0) {
            return sigma;
        }
        double median = baseline.getMedian() == null ? 0.0 : Math.abs(baseline.getMedian());
        double floor = Math.max(1.0, median * 0.1); // ≥1 unit, else 10% of the median
        return floor;
    }

    public static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(v, 1.0);
    }

    public static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }

    /** Human-readable duration: {@code 820ms}, {@code 1.2s}, {@code 1m 05s}. */
    public static String duration(double ms) {
        if (ms < 1000) {
            return Math.round(ms) + "ms";
        }
        double seconds = ms / 1000.0;
        if (seconds < 60) {
            return round(seconds, 1) + "s";
        }
        long m = (long) (seconds / 60);
        long s = Math.round(seconds - m * 60);
        return m + "m " + String.format("%02ds", s);
    }

    /** Human-readable byte size: {@code 512 B}, {@code 3.4 KB}, {@code 1.2 MB}. */
    public static String bytes(double b) {
        if (b < 1024) {
            return Math.round(b) + " B";
        }
        double kb = b / 1024.0;
        if (kb < 1024) {
            return round(kb, 1) + " KB";
        }
        return round(kb / 1024.0, 1) + " MB";
    }

    /**
     * Bounds a dedup key to the column's 128 chars deterministically: a long key keeps a
     * readable prefix and appends a stable hash of the whole thing, so two different long
     * keys still map to different bounded keys (no accidental aggregation).
     */
    public static String capKey(String key) {
        if (key == null) {
            return "";
        }
        if (key.length() <= 128) {
            return key;
        }
        String hash = Integer.toHexString(key.hashCode());
        String prefix = key.substring(0, 128 - hash.length() - 1);
        return prefix + "~" + hash;
    }
}
