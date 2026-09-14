package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Google Sheets: appends a row via Sheets API integration. */
@Component
public class GoogleSheetsExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "google_sheets";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String spreadsheetId = ctx.configString("spreadsheetId");
        String sheetName = ctx.configString("sheetName");
        Object rawRow = ctx.config().get("row");
        JsonNode rowNode = rawRow instanceof JsonNode ? (JsonNode) rawRow : null;
        if (spreadsheetId == null || spreadsheetId.isBlank()) return NodeResult.fail("Google Sheets requires 'spreadsheetId'.");
        if (sheetName == null || sheetName.isBlank()) return NodeResult.fail("Google Sheets requires 'sheetName'.");
        if (rowNode == null) return NodeResult.fail("Google Sheets requires 'row'.");

        String accessToken = ctx.secret("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("appended", false);
            output.put("reason", "No connected Google Sheets integration — configure an integration with API credentials.");
            ctx.log().warn("Google Sheets not appended: no access token.");
            return NodeResult.success(output);
        }

        // Build values array from row (object or array)
        com.fasterxml.jackson.databind.node.ArrayNode values = ctx.mapper().createArrayNode();
        if (rowNode.isArray()) {
            for (JsonNode cell : rowNode) {
                values.add(cell.asText());
            }
        } else if (rowNode.isObject()) {
            rowNode.fields().forEachRemaining(e -> values.add(e.getValue().asText()));
        }

        try {
            java.net.URI uri = java.net.URI.create(
                    "https://sheets.googleapis.com/v4/spreadsheets/" + spreadsheetId + "/values/" + sheetName + "!A1:append?valueInputOption=RAW");
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String payload = "{\"values\":[" + values.toString() + "]}";
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("appended", response.statusCode() >= 200 && response.statusCode() < 300);
            output.put("status", response.statusCode());
            ctx.log().info("Google Sheets appended: " + response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Google Sheets error: " + e.getMessage());
        }
    }
}