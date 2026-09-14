package com.flowops.integration.provider;

/**
 * Static capability flags for a provider. Used by the UI to show what a
 * provider can do and by the sync scheduler to choose the right ingestion
 * mode.
 */
public record ProviderCapabilities(
        boolean workflowDiscovery,
        boolean executionHistory,
        boolean nodeExecutionData,
        boolean incrementalSync,
        boolean webhooks,
        boolean tracing
) {
}