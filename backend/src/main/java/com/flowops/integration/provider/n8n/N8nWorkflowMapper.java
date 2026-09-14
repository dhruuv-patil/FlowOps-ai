package com.flowops.integration.provider.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExternalWorkflow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps n8n workflow responses to normalized {@link ExternalWorkflow} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 */
public final class N8nWorkflowMapper {

    private N8nWorkflowMapper() {
    }

    /**
     * Converts n8n list-workflows response to normalized workflows.
     * n8n response: { "data": [ { "id": "123", "name": "My Workflow", "active": true, "updatedAt": "..." }, ... ] }
     */
    public static List<ExternalWorkflow> toExternal(JsonNode response) {
        List<ExternalWorkflow> workflows = new ArrayList<>();
        JsonNode data = response.get("data");
        if (data != null && data.isArray()) {
            for (JsonNode w : data) {
                workflows.add(toExternalWorkflow(w));
            }
        }
        return workflows;
    }

    public static ExternalWorkflow toExternalWorkflow(JsonNode w) {
        String externalId = w.path("id").asText("");
        String name = w.path("name").asText("");
        boolean active = w.path("active").asBoolean(false);
        String status = active ? "active" : "inactive";
        String updatedAt = w.path("updatedAt").asText("");

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (!updatedAt.isEmpty()) {
            metadata.put("updatedAt", updatedAt);
        }
        // Preserve raw fields for debugging/future use
        metadata.put("raw", w);

        return new ExternalWorkflow(externalId, name, status, metadata);
    }
}