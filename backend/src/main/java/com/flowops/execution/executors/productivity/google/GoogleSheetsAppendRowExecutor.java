package com.flowops.execution.executors.productivity.google;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.provider.productivity.google.GoogleSheetsClient;
import org.springframework.stereotype.Component;

/** Google Sheets: appends a row. */
@Component
public class GoogleSheetsAppendRowExecutor implements NodeExecutor {
    private final GoogleSheetsClient client;

    public GoogleSheetsAppendRowExecutor(GoogleSheetsClient client) {
        this.client = client;
    }

    @Override public String type() { return "google_sheets:appendRow"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String spreadsheetId = ctx.configString("spreadsheetId");
        JsonNode rowData = ctx.mapper().valueToTree(ctx.config().get("row"));
        if (spreadsheetId == null || rowData == null || rowData.isNull())
            return NodeResult.fail("Google Sheets appendRow requires 'spreadsheetId' and 'row'.");

        String accessToken = ctx.secret("accessToken");
        if (accessToken == null) return NodeResult.fail("No Google Sheets Token configured.");

        try {
            String path = spreadsheetId + "/values/Sheet1:append?valueInputOption=USER_ENTERED";
            String payload = ctx.mapper().writeValueAsString(java.util.Map.of("values", List.of(rowData)));

            var response = client.execute(accessToken, "POST", path, payload);

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() == 200);
            output.put("status", response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Google Sheets API error: " + e.getMessage());
        }
    }
}
