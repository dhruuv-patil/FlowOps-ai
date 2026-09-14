package com.flowops.integration.provider.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExecutionStatus;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.SyncCursor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps n8n execution responses to normalized
 * {@link ExternalExecution} records.
 *
 * <p>Pure static functions — no HTTP, no state,
 * fully unit-testable.</p>
 */
public final class N8nExecutionMapper {

    private N8nExecutionMapper() {
    }

    /**
     * Converts an n8n list-executions response to normalized executions.
     *
     * <p>Expected n8n response:</p>
     *
     * <pre>
     * {
     *   "data": [
     *     {
     *       "id": "123",
     *       "workflowId": "456",
     *       "status": "success",
     *       "startedAt": "...",
     *       "stoppedAt": "..."
     *     }
     *   ],
     *   "nextCursor": "opaque-cursor"
     * }
     * </pre>
     */
    public static List<ExternalExecution> toExternal(
            JsonNode response) {

        List<ExternalExecution> executions =
                new ArrayList<>();

        if (response == null || response.isNull()) {
            return executions;
        }

        JsonNode data =
                response.get("data");

        if (data != null && data.isArray()) {

            for (JsonNode execution : data) {

                if (execution == null
                        || !execution.isObject()) {
                    continue;
                }

                ExternalExecution mapped =
                        toExternalExecution(execution);

                /*
                 * Ignore malformed records that don't contain
                 * an execution ID or workflow ID.
                 */
                if (!mapped.externalId().isBlank()
                        && !mapped.workflowExternalId().isBlank()) {

                    executions.add(mapped);
                }
            }
        }

        return executions;
    }

    /**
     * Extracts n8n's opaque pagination cursor.
     *
     * <p>This value must be passed unchanged to the next
     * {@code /executions?cursor=...} request.</p>
     */
    public static SyncCursor nextCursor(
            JsonNode response) {

        if (response == null || response.isNull()) {
            return SyncCursor.EMPTY;
        }

        JsonNode cursor =
                response.get("nextCursor");

        if (cursor == null
                || cursor.isNull()
                || !cursor.isTextual()) {

            return SyncCursor.EMPTY;
        }

        String value =
                cursor.asText().trim();

        if (value.isEmpty()) {
            return SyncCursor.EMPTY;
        }

        return new SyncCursor(value);
    }

    public static ExternalExecution toExternalExecution(
            JsonNode e) {

        String externalId =
                e.path("id").asText("");

        String workflowExternalId =
                e.path("workflowId").asText("");

        ExecutionStatus status =
                mapStatus(
                        e.path("status").asText(""));

        Instant startedAt =
                parseInstant(
                        e.path("startedAt"));

        Instant finishedAt =
                parseInstant(
                        e.path("stoppedAt"));

        Map<String, Object> metadata =
                new LinkedHashMap<>();

        metadata.put(
                "mode",
                e.path("mode").asText(""));

        metadata.put(
                "raw",
                e);

        return new ExternalExecution(
                externalId,
                workflowExternalId,
                status,
                startedAt,
                finishedAt,
                metadata);
    }

    /**
     * Maps n8n execution statuses to FlowOps statuses.
     *
     * <p>Package-visible so
     * {@link N8nNodeExecutionMapper} can reuse the
     * full-run status mapping.</p>
     */
    static ExecutionStatus mapStatus(
            String n8nStatus) {

        if (n8nStatus == null) {
            return ExecutionStatus.FAILED;
        }

        return switch (n8nStatus.toLowerCase()) {

            case "success" ->
                    ExecutionStatus.SUCCEEDED;

            case "error" ->
                    ExecutionStatus.FAILED;

            case "waiting" ->
                    ExecutionStatus.WAITING;

            case "running", "new" ->
                    ExecutionStatus.RUNNING;

            case "cancelled" ->
                    ExecutionStatus.CANCELED;

            default ->
                    ExecutionStatus.FAILED;
        };
    }

    private static Instant parseInstant(
            JsonNode node) {

        if (node == null
                || node.isNull()
                || node.isMissingNode()) {

            return null;
        }

        String text =
                node.asText();

        if (text == null
                || text.isBlank()) {

            return null;
        }

        try {
            return Instant.parse(text);

        } catch (Exception ignored) {
            return null;
        }
    }
}