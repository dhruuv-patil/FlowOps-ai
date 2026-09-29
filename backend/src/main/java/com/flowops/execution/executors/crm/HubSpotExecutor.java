package com.flowops.execution.executors.crm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** HubSpot node executor: creates/updates contacts and deals. */
@Component
public class HubSpotExecutor implements NodeExecutor {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String type() {
        return "hubspot";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String operation = ctx.configString("operation");
        if (operation == null || operation.isBlank()) {
            operation = "createContact";
        }

        String token = ctx.secret("accessToken");
        if (token == null || token.isBlank()) {
            return NodeResult.fail("No connected HubSpot integration — configure Private App Access Token.");
        }

        try {
            switch (operation.toLowerCase()) {
                case "createcontact":
                    return createContact(ctx, token);
                case "createdeal":
                    return createDeal(ctx, token);
                default:
                    return NodeResult.fail("Unknown HubSpot operation: " + operation);
            }
        } catch (Exception e) {
            return NodeResult.fail("HubSpot error: " + e.getMessage());
        }
    }

    private NodeResult createContact(NodeExecutionContext ctx, String token) throws Exception {
        String email = ctx.configString("email");
        String firstName = ctx.configString("firstName");
        String lastName = ctx.configString("lastName");
        String company = ctx.configString("company");

        if (email == null || email.isBlank()) {
            return NodeResult.fail("HubSpot createContact requires an 'email'.");
        }

        String payload = "{\"properties\":{";
        payload += "\"email\":\"" + escape(email) + "\"";
        if (firstName != null && !firstName.isBlank()) {
            payload += ",\"firstname\":\"" + escape(firstName) + "\"";
        }
        if (lastName != null && !lastName.isBlank()) {
            payload += ",\"lastname\":\"" + escape(lastName) + "\"";
        }
        if (company != null && !company.isBlank()) {
            payload += ",\"company\":\"" + escape(company) + "\"";
        }
        payload += "}}";

        URI uri = URI.create("https://api.hubapi.com/crm/v3/objects/contacts");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + token.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectNode output = ctx.mapper().createObjectNode();
        boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
        output.put("created", ok);
        output.put("status", response.statusCode());
        output.put("email", email);
        if (ok) {
            output.put("response", response.body());
        }

        if (ok) {
            ctx.log().info("HubSpot contact created for " + email + ": " + response.statusCode());
            return NodeResult.success(output);
        } else {
            return NodeResult.fail("HubSpot API failed (" + response.statusCode() + "): " + response.body());
        }
    }

    private NodeResult createDeal(NodeExecutionContext ctx, String token) throws Exception {
        String dealName = ctx.configString("dealName");
        String amount = ctx.configString("amount");
        String stage = ctx.configString("stage");

        if (dealName == null || dealName.isBlank()) {
            return NodeResult.fail("HubSpot createDeal requires a 'dealName'.");
        }

        String payload = "{\"properties\":{";
        payload += "\"dealname\":\"" + escape(dealName) + "\"";
        if (amount != null && !amount.isBlank()) {
            payload += ",\"amount\":\"" + escape(amount) + "\"";
        }
        if (stage != null && !stage.isBlank()) {
            payload += ",\"dealstage\":\"" + escape(stage) + "\"";
        }
        payload += "}}";

        URI uri = URI.create("https://api.hubapi.com/crm/v3/objects/deals");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + token.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectNode output = ctx.mapper().createObjectNode();
        boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
        output.put("created", ok);
        output.put("status", response.statusCode());
        output.put("dealName", dealName);
        if (ok) {
            output.put("response", response.body());
        }

        if (ok) {
            ctx.log().info("HubSpot deal created: " + dealName + " (" + response.statusCode() + ")");
            return NodeResult.success(output);
        } else {
            return NodeResult.fail("HubSpot API failed (" + response.statusCode() + "): " + response.body());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
