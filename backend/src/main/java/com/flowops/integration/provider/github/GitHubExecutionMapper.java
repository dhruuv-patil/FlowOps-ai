package com.flowops.integration.provider.github;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps GitHub run responses to normalized {@link ExternalExecution} records.
 * Pure static functions — no HTTP, no state, fully unit-testable.
 *
 * <p>Expected GitHub response (simplified):</p>
 * <pre>
 * {
 *   "total_count": 100,
 *   "workflow_runs": [
 *     {
 *       "id": 123456789,
 *       "workflow_id": 1234567,
 *       "conclusion": "success",
 *       "status": "completed",
 *       "created_at": "...",
 *       "updated_at": "...",
 *       "run_started_at": "...",
 *       "html_url": "https://github.com/owner/repo/actions/runs/123456789"
 *     }
 *   ]
 * }
 * </pre>
 */
public final class GitHubExecutionMapper {

    private GitHubExecutionMapper() {
    }

    public static List<ExternalExecution> toExternal(JsonNode response) {
        List<ExternalExecution> executions = new ArrayList<>();
        if (response == null || response.isNull()) {
            return executions;
        }

        JsonNode data = response.get("workflow_runs");
        if (data != null && data.isArray()) {
            for (JsonNode run : data) {
                if (run == null || !run.isObject()) {
                    continue;
                }
                ExternalExecution mapped = toExternalExecution(run);
                if (!mapped.externalId().isBlank()
                        && !mapped.workflowExternalId().isBlank()) {
                    executions.add(mapped);
                }
            }
        }
        return executions;
    }

    public static ExternalExecution toExternalExecution(JsonNode run) {
        String externalId = String.valueOf(run.path("id").asLong(0));
        String workflowExternalId = String.valueOf(run.path("workflow_id").asLong(0));
        ExecutionStatus status = mapStatus(
                run.path("conclusion").asText(""),
                run.path("status").asText(""));
        Instant startedAt = parseInstant(run.path("run_started_at"));
        Instant finishedAt = parseInstant(run.path("updated_at"));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("html_url", run.path("html_url").asText(""));
        metadata.put("event", run.path("event").asText(""));
        metadata.put("branch", run.path("head_branch").asText(""));
        metadata.put("sha", run.path("head_sha").asText(""));
        metadata.put("raw", run);
        return new ExternalExecution(
                externalId, workflowExternalId, status, startedAt, finishedAt, metadata);
    }

    /**
     * Extracts the pagination cursor from GitHub's Link header via the {@code after}
     * parameter on the next page URL. GitHub Actions uses cursor-based pagination.
     */
    public static SyncCursor nextCursor(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return SyncCursor.EMPTY;
        }
        // Link header format: <url>; rel="next", <url>; rel="last"
        // We want the URL with rel="next" and extract its 'after' param.
        Pattern nextPattern = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
        Matcher m = nextPattern.matcher(linkHeader);
        if (!m.find()) {
            return SyncCursor.EMPTY;
        }
        String nextUrl = m.group(1);
        // Extract after=... from the URL
        Pattern afterPattern = Pattern.compile("[?&]after=([^&]+)");
        Matcher am = afterPattern.matcher(nextUrl);
        if (am.find()) {
            return new SyncCursor(am.group(1));
        }
        return SyncCursor.EMPTY;
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
        // Fall back to status field if conclusion not set (run still in progress)
        if (status != null) {
            return switch (status.toLowerCase()) {
                case "completed" -> ExecutionStatus.FAILED; // without conclusion → failure
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