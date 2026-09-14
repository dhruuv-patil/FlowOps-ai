package com.flowops.integration.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ExecutionStatus;
import com.flowops.integration.provider.ExternalNodeExecution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps GitHub job/step responses to normalized {@link ExternalNodeExecution} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 *
 * <p>Expected GitHub response (simplified):</p>
 * <pre>
 * {
 *   "total_count": 3,
 *   "jobs": [
 *     {
 *       "id": 123456789,
 *       "run_id": 987654321,
 *       "workflow_name": "CI",
 *       "name": "build",
 *       "conclusion": "success",
 *       "status": "completed",
 *       "started_at": "...",
 *       "completed_at": "...",
 *       "steps": [
 *         { "name": "Checkout", "conclusion": "success", "started_at": "...", "completed_at": "..." },
 *         { "name": "Build", "conclusion": "success", "started_at": "...", "completed_at": "..." }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public final class GitHubNodeExecutionMapper {

    private GitHubNodeExecutionMapper() {
    }

    public static List<ExternalNodeExecution> toExternal(
            JsonNode response, String executionExternalId, String workflowExternalId) {
        List<ExternalNodeExecution> nodes = new ArrayList<>();
        if (response == null || response.isNull()) {
            return nodes;
        }

        JsonNode jobs = response.get("jobs");
        if (jobs != null && jobs.isArray()) {
            for (JsonNode job : jobs) {
                if (job == null || !job.isObject()) {
                    continue;
                }
                List<ExternalNodeExecution> mapped = toExternalNodeExecutions(
                        job, executionExternalId, workflowExternalId);
                nodes.addAll(mapped);
            }
        }
        return nodes;
    }

    private static List<ExternalNodeExecution> toExternalNodeExecutions(
            JsonNode job, String executionExternalId, String workflowExternalId) {
        List<ExternalNodeExecution> nodes = new ArrayList<>();
        // If steps are present, map each step; otherwise the job itself is one node.
        JsonNode steps = job.get("steps");
        if (steps != null && steps.isArray()) {
            for (JsonNode step : steps) {
                if (step == null || !step.isObject()) {
                    continue;
                }
                ExternalNodeExecution mapped = toExternalNodeExecution(
                        step, job, executionExternalId, workflowExternalId);
                if (!mapped.externalId().isBlank()) {
                    nodes.add(mapped);
                }
            }
        } else {
            // No steps → treat the job as a single node
            ExternalNodeExecution mapped = toExternalNodeExecution(
                    job, job, executionExternalId, workflowExternalId);
            if (!mapped.externalId().isBlank()) {
                nodes.add(mapped);
            }
        }
        return nodes;
    }

    private static ExternalNodeExecution toExternalNodeExecution(
            JsonNode step, JsonNode job, String executionExternalId, String workflowExternalId) {
        String externalId = step.path("id").asText("");
        if (externalId.isBlank()) {
            externalId = "job-" + job.path("id").asText("0");
        }
        String name = step.path("name").asText(job.path("name").asText("Unnamed Job"));
        ExecutionStatus status = mapStatus(
                step.path("conclusion").asText(""),
                step.path("status").asText(""));
        Instant startedAt = parseInstant(step.path("started_at"));
        if (startedAt == null) {
            startedAt = parseInstant(job.path("started_at"));
        }
        Instant finishedAt = parseInstant(step.path("completed_at"));
        if (finishedAt == null) {
            finishedAt = parseInstant(job.path("completed_at"));
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("job_name", job.path("name").asText(""));
        metadata.put("step_number", step.path("number").asInt(0));
        metadata.put("raw", step);
        return new ExternalNodeExecution(
                externalId, executionExternalId, workflowExternalId, name,
                status, startedAt, finishedAt,
                null, // GitHub doesn't expose input/output sizes
                null,
                metadata);
    }

    static ExecutionStatus mapStatus(String conclusion, String status) {
        if (conclusion != null && !conclusion.isBlank()) {
            return switch (conclusion.toLowerCase()) {
                case "success" -> ExecutionStatus.SUCCEEDED;
                case "failure", "cancelled", "canceled", "timed_out", "action_required" ->
                        ExecutionStatus.FAILED;
                case "neutral", "skipped" -> ExecutionStatus.SKIPPED;
                default -> ExecutionStatus.FAILED;
            };
        }
        if (status != null) {
            return switch (status.toLowerCase()) {
                case "completed" -> ExecutionStatus.FAILED;
                case "in_progress", "queued", "waiting" -> ExecutionStatus.RUNNING;
                default -> ExecutionStatus.RUNNING;
            };
        }
        return ExecutionStatus.RUNNING;
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