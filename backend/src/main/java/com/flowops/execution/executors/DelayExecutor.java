package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Delay: pauses the run for a fixed number of seconds, then continues. The wait is
 * clamped to {@code flowops.execution.max-delay-seconds} so a mis-configured node
 * can never park a worker indefinitely.
 */
@Component
public class DelayExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "delay";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws InterruptedException {
        double requested = readSeconds(ctx);
        if (requested < 0) {
            return NodeResult.fail("Delay seconds must be a non-negative number.");
        }
        double cap = ctx.properties().maxDelaySeconds();
        double waited = Math.min(requested, cap);

        if (waited > 0) {
            Thread.sleep((long) (waited * 1000));
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("requestedSeconds", requested);
        output.put("waitedSeconds", waited);
        if (waited < requested) {
            output.put("cappedAtSeconds", cap);
            ctx.log().warn("Delay of " + requested + "s clamped to the " + cap + "s limit.");
        } else {
            ctx.log().info("Delayed " + waited + "s.");
        }
        return NodeResult.success(output);
    }

    private double readSeconds(NodeExecutionContext ctx) {
        String raw = ctx.configString("seconds");
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }
}
