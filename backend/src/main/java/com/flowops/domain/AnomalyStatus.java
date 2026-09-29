package com.flowops.domain;

/** Lifecycle of an anomaly record. */
public enum AnomalyStatus {
    /** Newly detected; not yet handled. */
    OPEN,
    /** A human acknowledged it but it may still be on-going. */
    ACKNOWLEDGED,
    /**
     * A human triggered recovery verification; the system is now watching subsequent
     * executions to confirm the metric has returned to normal. The anomaly stays in
     * this state until enough healthy executions are observed (auto-resolves) or the
     * window closes without sufficient evidence (reverts to OPEN).
     */
    VERIFYING_RECOVERY,
    /** A human marked it resolved. */
    RESOLVED,
    /** A human marked it a false positive (training signal for future tuning). */
    FALSE_POSITIVE
}
