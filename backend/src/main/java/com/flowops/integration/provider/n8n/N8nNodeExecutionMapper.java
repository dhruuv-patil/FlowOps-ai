package com.flowops.integration.provider.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExecutionStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps n8n execution data (node results) to normalized {@link ExternalNodeExecution} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 */
public final class N8nNodeExecutionMapper {

    private N8nNodeExecutionMapper() {
    }

    /**
     * Converts n8n get-execution-data response to normalized node executions.
     * n8n response structure:
     * {
     *   "data": {
     *     "resultData": {
     *       "runData": {
     *         "Webhook": [{ "startTime": 123, "executionTime": 45, "executionStatus": "success", "error": null, "outputSize": 1024 }],
     *         "HTTP Request": [...]
     *       }
     *     },
     *     "status": "success"
     *   }
     * }
     */
    public static List<ExternalNodeExecution> toExternal(
            JsonNode response, String executionExternalId, String workflowExternalId) {

        List<ExternalNodeExecution> nodes = new ArrayList<>();
        JsonNode resultData = response.path("data").path("resultData");
        JsonNode runData = resultData.path("runData");

        if (runData.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = runData.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String nodeName = entry.getKey();
                JsonNode nodeRuns = entry.getValue();

                if (nodeRuns.isArray()) {
                    for (JsonNode run : nodeRuns) {
                        nodes.add(toExternal(run, nodeName, executionExternalId, workflowExternalId));
                    }
                }
            }
        }

        // If no runData, fall back to a single pseudo-node representing the whole execution
        if (nodes.isEmpty()) {
            ExecutionStatus overall = N8nExecutionMapper.mapStatus(
                    response.path("data").path("status").asText(""));
            nodes.add(new ExternalNodeExecution(
                    "execution",
                    executionExternalId,
                    workflowExternalId,
                    "Execution",
                    overall,
                    null, // startedAt - not available at this level
                    null, // finishedAt
                    null, // inputSize
                    null, // outputSize
                    Map.of("raw", response)
            ));
        }

        return nodes;
    }

    public static ExternalNodeExecution toExternal(
            JsonNode run, String nodeName, String executionExternalId, String workflowExternalId) {

        String externalId = nodeName; // n8n uses node name as the identifier within a run
        ExecutionStatus status = mapNodeStatus(run.path("executionStatus").asText(""));
        Instant startedAt = parseInstantFromMs(run.path("startTime")).orElse(null);
        Instant finishedAt = parseInstantFromMs(run.path("startTime"))
                .map(t -> t.plusMillis(run.path("executionTime").asLong(0)))
                .orElse(null);
        Long inputSize = null; // n8n doesn't expose input size in the list API
        Long outputSize = run.path("outputSize").asLong(); // may be 0 if not present

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("raw", run);

        return new ExternalNodeExecution(
                externalId,
                executionExternalId,
                workflowExternalId,
                nodeName,
                status,
                startedAt,
                finishedAt,
                inputSize,
                outputSize,
                metadata);
    }

    private static ExecutionStatus mapNodeStatus(String n8nStatus) {
        return switch (n8nStatus.toLowerCase()) {
            case "success" -> ExecutionStatus.SUCCEEDED;
            case "error" -> ExecutionStatus.FAILED;
            case "waiting" -> ExecutionStatus.WAITING;
            case "running" -> ExecutionStatus.RUNNING;
            case "cancelled" -> ExecutionStatus.CANCELED;
            default -> ExecutionStatus.FAILED;
        };
    }

    private static java.util.Optional<Instant> parseInstantFromMs(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return java.util.Optional.empty();
        }
        long ms = node.asLong(0);
        if (ms <= 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Instant.ofEpochMilli(ms));
    }
}