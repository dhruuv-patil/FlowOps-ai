package com.flowops.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Normalized representation of a remote workflow.
 * Provider-neutral — no n8n/Make/Zapier specifics leak past the provider boundary.
 */
public record ExternalWorkflow(
        String externalId,
        String name,
        String status,
        Map<String, Object> metadata
) {
    public ExternalWorkflow {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}