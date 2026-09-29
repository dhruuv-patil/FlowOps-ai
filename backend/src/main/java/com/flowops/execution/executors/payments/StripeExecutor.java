package com.flowops.execution.executors.payments;

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

/** Stripe node executor: retrieves customer, lists balance transactions. */
@Component
public class StripeExecutor implements NodeExecutor {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String type() {
        return "stripe";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String operation = ctx.configString("operation");
        if (operation == null || operation.isBlank()) {
            operation = "retrieveCustomer";
        }

        String secretKey = ctx.secret("secretKey");
        if (secretKey == null || secretKey.isBlank()) {
            return NodeResult.fail("No connected Stripe integration — configure a Secret Key.");
        }

        try {
            switch (operation.toLowerCase()) {
                case "retrievecustomer":
                    return retrieveCustomer(ctx, secretKey);
                case "listbalancetransactions":
                    return listBalanceTransactions(ctx, secretKey);
                default:
                    return NodeResult.fail("Unknown Stripe operation: " + operation);
            }
        } catch (Exception e) {
            return NodeResult.fail("Stripe error: " + e.getMessage());
        }
    }

    private NodeResult retrieveCustomer(NodeExecutionContext ctx, String secretKey) throws Exception {
        String customerId = ctx.configString("customerId");
        if (customerId == null || customerId.isBlank()) {
            return NodeResult.fail("Stripe retrieveCustomer requires a 'customerId'.");
        }

        URI uri = URI.create("https://api.stripe.com/v1/customers/" + customerId.trim());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + secretKey.trim())
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectNode output = ctx.mapper().createObjectNode();
        boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
        output.put("status", response.statusCode());
        output.put("customerId", customerId);
        if (ok) {
            output.put("customer", response.body());
        }

        if (ok) {
            ctx.log().info("Stripe customer retrieved: " + customerId);
            return NodeResult.success(output);
        } else {
            return NodeResult.fail("Stripe API failed (" + response.statusCode() + "): " + response.body());
        }
    }

    private NodeResult listBalanceTransactions(NodeExecutionContext ctx, String secretKey) throws Exception {
        String limit = ctx.configString("limit");
        String url = "https://api.stripe.com/v1/balance_transactions?limit=" + (limit != null ? limit.trim() : "10");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + secretKey.trim())
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectNode output = ctx.mapper().createObjectNode();
        boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
        output.put("status", response.statusCode());
        if (ok) {
            output.put("transactions", response.body());
        }

        if (ok) {
            ctx.log().info("Stripe balance transactions listed: " + response.statusCode());
            return NodeResult.success(output);
        } else {
            return NodeResult.fail("Stripe API failed (" + response.statusCode() + "): " + response.body());
        }
    }
}
