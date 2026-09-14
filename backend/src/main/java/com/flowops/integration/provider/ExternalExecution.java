package com.flowops.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;

/**
 * Normalized representation of a remote workflow execution (run).
 * Provider-neutral — no n8n/Make/Zapier specifics leak past the provider boundary.
 */
public record ExternalExecution(
        String externalId,
        String workflowExternalId,
        ExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> metadata
) {
    public ExternalExecution {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId cannot be blank");
        }
        if (workflowExternalId == null || workflowExternalId.isBlank()) {
            throw new IllegalArgumentException("workflowExternalId cannot be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
    }
}