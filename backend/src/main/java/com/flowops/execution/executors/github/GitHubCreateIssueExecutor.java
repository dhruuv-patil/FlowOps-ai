package com.flowops.execution.executors.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.provider.github.GitHubClient;
import org.springframework.stereotype.Component;

/** GitHub: creates an issue. */
@Component
public class GitHubCreateIssueExecutor implements NodeExecutor {
    private final GitHubClient client;

    public GitHubCreateIssueExecutor(GitHubClient client) {
        this.client = client;
    }

    @Override public String type() { return "github_create_issue"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String repo = ctx.configString("repo");
        String title = ctx.configString("title");
        String body = ctx.configString("body");
        if (repo == null || title == null) return NodeResult.fail("GitHub createIssue requires 'repo' and 'title'.");

        String pat = ctx.secret("integrationId"); // Ideally integrationId -> pat lookup
        if (pat == null) return NodeResult.fail("No PAT configured.");

        try {
            String payload = "{\"title\":\"" + ctx.interpolate(title).replace("\"", "\\\"") + "\"" +
                    (body != null ? ",\"body\":\"" + ctx.interpolate(body).replace("\"", "\\\"") + "\"" : "") + "}";

            var response = client.execute(pat, "https://api.github.com", "POST", "/repos/" + repo + "/issues", payload);

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            output.put("status", response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode resp = ctx.mapper().readTree(response.body());
                output.put("issueNumber", resp.path("number").asInt(0));
            }
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail(e.getMessage());
        }
    }
}
