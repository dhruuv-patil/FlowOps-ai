package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Webhook Response: shapes an outbound HTTP response (status, content type, body, and
 * headers) without making any network call. This is a response builder meant to live at
 * the tail of a webhook-driven workflow so the inbound caller receives a well-formed payload.
 */
@Component
public class WebhookResponseExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "webhook_response";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        int status = parseStatus(ctx.configString("status") != null
                ? ctx.configString("status")
                : ctx.configString("statusCode"));

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("status", status);
        output.put("ok", status >= 200 && status < 300);
        output.put("contentType", orDefault(ctx.configString("contentType"), "application/json"));
        output.set("body", body(ctx));
        output.set("headers", headers(ctx));

        ctx.log().info("Webhook Response shaped → " + status + ".");
        return NodeResult.success(output);
    }

    private JsonNode body(NodeExecutionContext ctx) {
        Object rawBody = ctx.config().get("body");
        if (rawBody == null) {
            return ctx.mapper().createObjectNode();
        }
        if (rawBody instanceof JsonNode node) {
            return node.deepCopy();
        }
        String text = ctx.interpolate(String.valueOf(rawBody));
        if (text.isBlank()) {
            return ctx.mapper().createObjectNode();
        }
        try {
            return ctx.mapper().readTree(text);
        } catch (Exception notJson) {
            return ctx.mapper().getNodeFactory().textNode(text);
        }
    }

    private ObjectNode headers(NodeExecutionContext ctx) {
        ObjectNode headers = ctx.mapper().createObjectNode();
        Object raw = ctx.config().get("headers");
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null && v != null) {
                    headers.put(String.valueOf(k), ctx.interpolate(String.valueOf(v)));
                }
            });
        } else if (raw instanceof String text && !text.isBlank()) {
            try {
                JsonNode parsed = ctx.mapper().readTree(text);
                if (parsed.isObject()) {
                    parsed.fields().forEachRemaining(e ->
                            headers.put(e.getKey(), e.getValue().isValueNode() ? e.getValue().asText() : e.getValue().toString()));
                }
            } catch (Exception ignored) {
            }
        }
        return headers;
    }

    private int parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return 200;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException nfe) {
            return 200;
        }
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}