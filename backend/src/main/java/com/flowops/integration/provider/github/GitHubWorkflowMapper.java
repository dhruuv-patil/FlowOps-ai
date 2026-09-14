package com.flowops.integration.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExternalWorkflow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Maps GitHub workflow responses to normalized {@link ExternalWorkflow} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 *
 * <p>Expected GitHub response (simplified):</p>
 * <pre>
 * {
 *   "total_count": 5,
 *   "workflows": [
 *     {
 *       "id": 1234567,
 *       "name": "CI",
 *       "path": ".github/workflows/ci.yml",
 *       "state": "active",
 *       "created_at": "...",
 *       "updated_at": "...",
 *       "html_url": "https://github.com/owner/repo/actions/workflows/1234567"
 *     }
 *   ]
 * }
 * </pre>
 */
public final class GitHubWorkflowMapper {

    private GitHubWorkflowMapper() {
    }

    public static List<ExternalWorkflow> toExternal(JsonNode response) {
        List<ExternalWorkflow> workflows = new ArrayList<>();
        if (response == null || response.isNull()) {
            return workflows;
        }

        JsonNode data = response.get("workflows");
        if (data != null && data.isArray()) {
            for (JsonNode workflow : data) {
                if (workflow == null || !workflow.isObject()) {
                    continue;
                }
                ExternalWorkflow mapped = toExternalWorkflow(workflow);
                if (!mapped.externalId().isBlank()) {
                    workflows.add(mapped);
                }
            }
        }
        return workflows;
    }

    public static ExternalWorkflow toExternalWorkflow(JsonNode w) {
        String externalId = String.valueOf(w.path("id").asLong(0));
        String name = w.path("name").asText("Unnamed Workflow");
        String status = w.path("state").asText("UNKNOWN");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("path", w.path("path").asText(""));
        metadata.put("html_url", w.path("html_url").asText(""));
        metadata.put("created_at", w.path("created_at").asText(""));
        metadata.put("updated_at", w.path("updated_at").asText(""));
        metadata.put("state", status);
        return new ExternalWorkflow(externalId, name, status, metadata);
    }
}