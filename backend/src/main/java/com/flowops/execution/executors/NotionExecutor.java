package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Notion: creates a page in a Notion database via Notion API. */
@Component
public class NotionExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "notion";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String databaseId = ctx.configString("databaseId");
        Object rawProps = ctx.config().get("properties");
        JsonNode propertiesNode = rawProps instanceof JsonNode ? (JsonNode) rawProps : null;
        if (databaseId == null || databaseId.isBlank()) return NodeResult.fail("Notion requires 'databaseId'.");
        if (propertiesNode == null) return NodeResult.fail("Notion requires 'properties' object.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null || apiToken.isBlank()) {
            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("created", false);
            output.put("reason", "No connected Notion integration — configure an integration with an API token.");
            ctx.log().warn("Notion not executed: no API token.");
            return NodeResult.success(output);
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String payload = "{\"parent\":{\"database_id\":\"" + databaseId + "\"},\"properties\":" + propertiesNode.toString() + "}";

            java.net.URI uri = java.net.URI.create("https://api.notion.com/v1/pages");
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Notion-Version", "2022-06-28")
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("created", response.statusCode() >= 200 && response.statusCode() < 300);
            output.put("status", response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode resp = ctx.mapper().readTree(response.body());
                output.put("pageId", resp.path("id").asText(""));
                output.put("pageUrl", resp.path("url").asText(""));
            }
            ctx.log().info("Notion createPage: " + response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Notion error: " + e.getMessage());
        }
    }
}