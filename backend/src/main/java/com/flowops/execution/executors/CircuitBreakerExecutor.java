package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Circuit Breaker: opens/closes based on metric vs threshold. */
@Component
public class CircuitBreakerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "circuit_breaker";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String metricPath = ctx.configString("metricPath");
        String thresholdStr = ctx.configString("threshold");
        if (metricPath == null || metricPath.isBlank()) {
            return NodeResult.fail("Circuit Breaker requires a 'metricPath'.");
        }
        if (thresholdStr == null || thresholdStr.isBlank()) {
            return NodeResult.fail("Circuit Breaker requires a 'threshold'.");
        }

        double threshold;
        try {
            threshold = Double.parseDouble(thresholdStr);
        } catch (NumberFormatException nfe) {
            return NodeResult.fail("Circuit Breaker 'threshold' must be a number.");
        }

        JsonNode metricNode = ctx.resolve(metricPath);
        if (metricNode.isMissingNode() || metricNode.isNull()) {
            return NodeResult.fail("Circuit Breaker metric path '" + metricPath + "' not found.");
        }

        double metric;
        if (metricNode.isNumber()) {
            metric = metricNode.asDouble();
        } else {
            try {
                metric = Double.parseDouble(metricNode.asText());
            } catch (NumberFormatException nfe) {
                return NodeResult.fail("Circuit Breaker metric at '" + metricPath + "' is not a number.");
            }
        }

        boolean open = metric > threshold;
        String handle = open ? "open" : "closed";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("metric", metric);
        output.put("threshold", threshold);
        output.put("state", handle);

        ctx.log().info("Circuit Breaker: metric=" + metric + " threshold=" + threshold + " → " + handle + ".");
        return NodeResult.success(output, java.util.List.of(handle));
    }
}