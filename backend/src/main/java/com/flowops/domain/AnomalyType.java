package com.flowops.domain;

/**
 * The four detectable anomaly families (reliability pivot).
 *
 * <p>{@link #BEHAVIORAL} covers "the workflow succeeded but walked a different path
 * than normal" (e.g. a Slack node that usually fires did not). {@link #OUTPUT} is the
 * "successful but wrong" family — the run succeeded but produced abnormal output.
 */
public enum AnomalyType {
    VOLUME,
    LATENCY,
    OUTPUT,
    BEHAVIORAL
}
