package com.flowops.integration.provider.make;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExecutionStatus;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.SyncCursor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Make execution responses to normalized {@link ExternalExecution} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 *
 * <p>Expected Make response (simplified):</p>
 * <pre>
 * {
 *   "executions": [
 *     {
 *       "id": 12345,
 *       "scenarioId": 123,
 *       "status": "success",
 *       "startedAt": "...",
 *       "finishedAt": "...",
 *       "duration": 1234,
 *       "dataSize": 1024
 *     }
 *   ]
 * }
 * </pre>
 */
public final class MakeExecutionMapper {

    private MakeExecutionMapper() {
    }

    public static List<ExternalExecution> toExternal(JsonNode response) {
        List<ExternalExecution> executions = new ArrayList<>();
        if (response == null || response.isNull()) {
            return executions;
        }

        JsonNode data = response.get("executions");
        if (data != null && data.isArray()) {
            for (JsonNode execution : data) {
                if (execution == null || !execution.isObject()) {
                    continue;
                }
                ExternalExecution mapped = toExternalExecution(execution);
                if (!mapped.externalId().isBlank()
                        && !mapped.workflowExternalId().isBlank()) {
                    executions.add(mapped);
                }
            }
        }
        return executions;
    }

    public static ExternalExecution toExternalExecution(JsonNode e) {
        String externalId = String.valueOf(e.path("id").asLong(0));
        String workflowExternalId = String.valueOf(e.path("scenarioId").asLong(0));
        ExecutionStatus status = mapStatus(e.path("status").asText(""));
        Instant startedAt = parseInstant(e.path("startedAt"));
        Instant finishedAt = parseInstant(e.path("finishedAt"));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("durationMs", e.path("duration").asLong(0));
        metadata.put("dataSizeBytes", e.path("dataSize").asLong(0));
        metadata.put("incomplete", e.path("incomplete").asBoolean(false));
        metadata.put("raw", e);
        return new ExternalExecution(
                externalId, workflowExternalId, status, startedAt, finishedAt, metadata);
    }

    static ExecutionStatus mapStatus(String makeStatus) {
        if (makeStatus == null) {
            return ExecutionStatus.FAILED;
        }
        return switch (makeStatus.toLowerCase()) {
            case "success", "succeeded" -> ExecutionStatus.SUCCEEDED;
            case "error", "failed", "warning" -> ExecutionStatus.FAILED;
            case "running", "pending" -> ExecutionStatus.RUNNING;
            case "cancelled", "canceled" -> ExecutionStatus.CANCELED;
            default -> ExecutionStatus.FAILED;
        };
    }

    private static Instant parseInstant(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }
}