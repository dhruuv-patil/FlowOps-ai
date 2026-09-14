package com.flowops.integration.provider.make;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExternalWorkflow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Maps Make scenario responses to normalized {@link ExternalWorkflow} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 *
 * <p>Expected Make response (simplified):</p>
 * <pre>
 * {
 *   "scenarios": [
 *     {
 *       "id": 123,
 *       "name": "My Scenario",
 *       "status": "ACTIVE",
 *       "createdAt": "...",
 *       "updatedAt": "..."
 *     }
 *   ]
 * }
 * </pre>
 */
public final class MakeWorkflowMapper {

    private MakeWorkflowMapper() {
    }

    public static List<ExternalWorkflow> toExternal(JsonNode response) {
        List<ExternalWorkflow> workflows = new ArrayList<>();
        if (response == null || response.isNull()) {
            return workflows;
        }

        JsonNode data = response.get("scenarios");
        if (data != null && data.isArray()) {
            for (JsonNode scenario : data) {
                if (scenario == null || !scenario.isObject()) {
                    continue;
                }
                ExternalWorkflow mapped = toExternalWorkflow(scenario);
                if (!mapped.externalId().isBlank()) {
                    workflows.add(mapped);
                }
            }
        }
        return workflows;
    }

    public static ExternalWorkflow toExternalWorkflow(JsonNode s) {
        String externalId = String.valueOf(s.path("id").asLong(0));
        String name = s.path("name").asText("Unnamed Scenario");
        String status = s.path("status").asText("UNKNOWN");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("folderId", s.path("folderId").asText(""));
        metadata.put("status", status);
        metadata.put("createdAt", s.path("createdAt").asText(""));
        metadata.put("updatedAt", s.path("updatedAt").asText(""));
        return new ExternalWorkflow(externalId, name, status, metadata);
    }
}