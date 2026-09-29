package com.flowops.execution.executors.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.provider.jira.JiraClient;
import org.springframework.stereotype.Component;

/** Jira: creates an issue. */
@Component
public class JiraCreateIssueExecutor implements NodeExecutor {
    private final JiraClient client;

    public JiraCreateIssueExecutor(JiraClient client) {
        this.client = client;
    }

    @Override public String type() { return "jira:createIssue"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String siteUrl = ctx.configString("siteUrl");
        String project = ctx.configString("project");
        String summary = ctx.configString("summary");
        if (siteUrl == null || project == null || summary == null) return NodeResult.fail("Jira createIssue requires 'siteUrl', 'project', and 'summary'.");

        String email = ctx.secret("email");
        String apiToken = ctx.secret("apiToken");
        if (email == null || apiToken == null) return NodeResult.fail("Jira credentials not configured.");

        try {
            String payload = ctx.mapper().writeValueAsString(java.util.Map.of(
                "fields", java.util.Map.of(
                    "project", java.util.Map.of("key", project),
                    "summary", ctx.interpolate(summary),
                    "issuetype", java.util.Map.of("name", "Task")
                )
            ));

            var response = client.execute(siteUrl, email, apiToken, "POST", "issue", payload);

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() == 201);
            output.put("status", response.statusCode());
            if (response.statusCode() == 201) {
                JsonNode resp = ctx.mapper().readTree(response.body());
                output.put("issueKey", resp.path("key").asText(""));
            }
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Jira API error: " + e.getMessage());
        }
    }
}
