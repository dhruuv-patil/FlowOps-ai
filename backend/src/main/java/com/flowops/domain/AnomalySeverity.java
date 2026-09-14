package com.flowops.domain;

/**
 * Explainable severity bands for an anomaly, derived from the magnitude of the
 * deviation, how many executions were affected, downstream impact, and confidence.
 * The exact thresholds live in {@code ReliabilityProperties}; the chosen band is
 * recorded in the anomaly's evidence so a business user can see the reasoning.
 */
public enum AnomalySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
