package com.flowops.domain;

/** Lifecycle of an anomaly record. */
public enum AnomalyStatus {
    /** Newly detected; not yet handled. */
    OPEN,
    /** A human acknowledged it but it may still be on-going. */
    ACKNOWLEDGED,
    /** A human marked it resolved. */
    RESOLVED,
    /** A human marked it a false positive (training signal for future tuning). */
    FALSE_POSITIVE
}
