package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Timeout: fails if an upstream duration exceeds a budget. */
@Component
public class TimeoutExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "timeout";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String durationPath = ctx.configString("durationPath");
        String timeoutStr = ctx.configString("timeoutMs");
        if (durationPath == null || durationPath.isBlank()) {
            return NodeResult.fail("Timeout requires a 'durationPath'.");
        }
        if (timeoutStr == null || timeoutStr.isBlank()) {
            return NodeResult.fail("Timeout requires a 'timeoutMs'.");
        }

        long timeoutMs;
        try {
            timeoutMs = Long.parseLong(timeoutStr);
        } catch (NumberFormatException nfe) {
            return NodeResult.fail("Timeout 'timeoutMs' must be a number.");
        }

        JsonNode durationNode = ctx.resolve(durationPath);
        if (durationNode.isMissingNode() || durationNode.isNull()) {
            return NodeResult.fail("Timeout duration path '" + durationPath + "' not found.");
        }

        long durationMs;
        if (durationNode.isNumber()) {
            durationMs = durationNode.asLong();
        } else {
            try {
                durationMs = Long.parseLong(durationNode.asText());
            } catch (NumberFormatException nfe) {
                return NodeResult.fail("Timeout duration at '" + durationPath + "' is not a number.");
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("durationMs", durationMs);
        output.put("timeoutMs", timeoutMs);
        output.put("exceeded", durationMs > timeoutMs);

        if (durationMs > timeoutMs) {
            ctx.log().error("Timeout exceeded: " + durationMs + "ms > " + timeoutMs + "ms.");
            return NodeResult.fail("Duration " + durationMs + "ms exceeds timeout " + timeoutMs + "ms.");
        }

        ctx.log().info("Timeout check passed: " + durationMs + "ms <= " + timeoutMs + "ms.");
        return NodeResult.success(output);
    }
}