package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Health Check: evaluates checks against values, branches healthy/unhealthy. */
@Component
public class HealthCheckExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "health_check";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawChecks = ctx.config().get("checks");
        JsonNode checksNode = rawChecks instanceof JsonNode ? (JsonNode) rawChecks : null;
        if (checksNode == null || !checksNode.isArray()) {
            return NodeResult.fail("Health Check requires a 'checks' array.");
        }

        boolean allHealthy = true;
        ObjectNode results = ctx.mapper().createObjectNode();

        for (JsonNode check : checksNode) {
            String path = check.path("path").asText();
            String op = check.path("op").asText("==");
            String value = check.path("value").asText();

            if (path.isBlank()) continue;

            JsonNode actualNode = ctx.resolve(path);
            if (actualNode.isMissingNode() || actualNode.isNull()) {
                allHealthy = false;
                results.put(path, "missing");
                continue;
            }

            String actual = actualNode.isTextual() ? actualNode.asText() : actualNode.toString();
            boolean ok = switch (op) {
                case "==" -> actual.equals(value);
                case "!=" -> !actual.equals(value);
                case ">" -> {
                    try { yield Double.parseDouble(actual) > Double.parseDouble(value); }
                    catch (NumberFormatException e) { yield false; }
                }
                case "<" -> {
                    try { yield Double.parseDouble(actual) < Double.parseDouble(value); }
                    catch (NumberFormatException e) { yield false; }
                }
                case "contains" -> actual.contains(value);
                default -> false;
            };

            if (!ok) allHealthy = false;
            results.put(path, ok);
        }

        String handle = allHealthy ? "healthy" : "unhealthy";
        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("checks", results);
        output.put("healthy", allHealthy);
        ctx.log().info("Health Check: " + handle);
        return NodeResult.success(output, java.util.List.of(handle));
    }
}