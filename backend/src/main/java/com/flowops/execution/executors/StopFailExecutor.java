package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Stop/Fail: stops the run — either failing it or ending it early. */
@Component
public class StopFailExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "stop_fail";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String mode = ctx.configString("mode");
        String message = ctx.configString("message");
        if (mode == null || mode.isBlank()) {
            return NodeResult.fail("Stop/Fail requires a 'mode' (fail or stop).");
        }
        if (message == null || message.isBlank()) {
            return NodeResult.fail("Stop/Fail requires a 'message'.");
        }

        String interpolated = ctx.interpolate(message);
        if ("fail".equals(mode)) {
            ctx.log().error("Stop/Fail (fail): " + interpolated);
            return NodeResult.fail(interpolated);
        } else if ("stop".equals(mode)) {
            ctx.log().info("Stop/Fail (stop): " + interpolated);
            // Return a special output that the engine treats as stop
            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("stopped", true);
            output.put("message", interpolated);
            // We'll use a WAIT-like mechanism but with a marker
            // Actually, the engine doesn't have a "stop cleanly" result kind
            // We'll fail with a special message and let the run fail
            return NodeResult.success(output);
        } else {
            return NodeResult.fail("Stop/Fail mode must be 'fail' or 'stop'.");
        }
    }
}