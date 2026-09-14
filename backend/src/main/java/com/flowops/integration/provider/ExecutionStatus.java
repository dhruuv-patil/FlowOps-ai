package com.flowops.integration.provider;

/**
 * Normalized execution status across providers.
 * Mappers translate provider-specific statuses to these values.
 */
public enum ExecutionStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    WAITING,
    SKIPPED,
    CANCELED
}