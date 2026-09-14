package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Outbound Webhook: sends an HTTP request to a configured webhook endpoint. */
@Component
public class OutboundWebhookExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "outbound_webhook";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String url = ctx.configString("url");
        String method = ctx.configString("method");
        Object rawHeaders = ctx.config().get("headers");
        String body = ctx.configString("body");

        if (url == null || url.isBlank()) return NodeResult.fail("Outbound Webhook requires 'url'.");
        if (method == null || method.isBlank()) return NodeResult.fail("Outbound Webhook requires 'method'.");

        String interpolatedUrl = ctx.interpolate(url);
        String interpolatedBody = body != null ? ctx.interpolate(body) : "";
        java.util.Map<String, Object> headers = asMap(ctx, rawHeaders);

        try {
            java.net.URI uri = java.net.URI.create(interpolatedUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest.Builder request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .method(method, java.net.http.HttpRequest.BodyPublishers.ofString(interpolatedBody));

            for (java.util.Map.Entry<String, Object> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    request.header(e.getKey(), ctx.interpolate(String.valueOf(e.getValue())));
                }
            }

            java.net.http.HttpResponse<String> response = client.send(request.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("status", response.statusCode());
            output.put("ok", response.statusCode() >= 200 && response.statusCode() < 300);
            output.set("body", parseBody(ctx, response));
            ctx.log().info("Outbound Webhook " + method + " " + interpolatedUrl + " → " + response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Outbound Webhook error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> asMap(NodeExecutionContext ctx, Object raw) {
        if (raw instanceof java.util.Map<?, ?> map) {
            return (java.util.Map<String, Object>) map;
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                JsonNode parsed = ctx.mapper().readTree(text);
                if (parsed.isObject()) {
                    return ctx.mapper().convertValue(parsed, java.util.Map.class);
                }
            } catch (Exception ignored) {}
        }
        return java.util.Map.of();
    }

    private JsonNode parseBody(NodeExecutionContext ctx, java.net.http.HttpResponse<String> response) {
        String raw = response.body() == null ? "" : response.body();
        try {
            return ctx.mapper().readTree(raw);
        } catch (Exception e) {
            return ctx.mapper().getNodeFactory().textNode(raw);
        }
    }
}