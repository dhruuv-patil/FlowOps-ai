package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Wait: pauses the workflow for an interpolated duration. */
@Component
public class WaitExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "wait";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws InterruptedException {
        String durationStr = ctx.configString("durationSeconds");
        if (durationStr == null || durationStr.isBlank()) {
            return NodeResult.fail("Wait requires a 'durationSeconds' value.");
        }

        double requested;
        try {
            requested = Double.parseDouble(ctx.interpolate(durationStr).trim());
        } catch (NumberFormatException nfe) {
            return NodeResult.fail("Wait 'durationSeconds' must be a number.");
        }
        if (requested < 0) {
            return NodeResult.fail("Wait duration must be non-negative.");
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
            ctx.log().warn("Wait of " + requested + "s clamped to the " + cap + "s limit.");
        } else {
            ctx.log().info("Waited " + waited + "s.");
        }
        return NodeResult.success(output);
    }
}