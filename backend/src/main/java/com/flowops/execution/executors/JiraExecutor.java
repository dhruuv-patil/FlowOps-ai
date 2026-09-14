package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Jira: creates a Jira issue via Jira Cloud REST API. */
@Component
public class JiraExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "jira";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String siteUrl = ctx.configString("siteUrl");
        String project = ctx.configString("project");
        String issueType = ctx.configString("issueType");
        String summary = ctx.configString("summary");
        String description = ctx.configString("description");
        if (siteUrl == null || siteUrl.isBlank()) return NodeResult.fail("Jira requires 'siteUrl'.");
        if (project == null || project.isBlank()) return NodeResult.fail("Jira requires 'project' (project key).");
        if (summary == null || summary.isBlank()) return NodeResult.fail("Jira requires 'summary'.");

        String email = ctx.secret("email");
        String apiToken = ctx.secret("apiToken");
        if (email == null || email.isBlank() || apiToken == null || apiToken.isBlank()) {
            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", false);
            output.put("reason", "No connected Jira integration — configure with email and API token.");
            ctx.log().warn("Jira not executed: no credentials.");
            return NodeResult.success(output);
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String payload = "{\"fields\":{\"project\":{\"key\":\"" + project + "\"},\"summary\":\"" +
                    ctx.interpolate(summary).replace("\"", "\\\"") + "\"" +
                    (issueType != null ? ",\"issuetype\":{\"name\":\"" + issueType + "\"}" : "") +
                    (description != null ? ",\"description\":\"" + ctx.interpolate(description).replace("\"", "\\\"") + "\"" : "") +
                    "}}";

            java.net.URI uri = java.net.URI.create(siteUrl.replaceAll("/+$", "") + "/rest/api/3/issue");
            String auth = java.util.Base64.getEncoder().encodeToString((email + ":" + apiToken).getBytes());
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            output.put("status", response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode resp = ctx.mapper().readTree(response.body());
                output.put("issueKey", resp.path("key").asText(""));
            }
            ctx.log().info("Jira createIssue: " + response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Jira error: " + e.getMessage());
        }
    }
}