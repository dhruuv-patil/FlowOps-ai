package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Try/Catch: passes through payload, logs if logErrors=true. */
@Component
public class TryCatchExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "try_catch";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String passThrough = ctx.configString("passThrough");
        boolean logErrors = Boolean.TRUE.equals(ctx.config().get("logErrors"));

        JsonNode output = ctx.mapper().createObjectNode();
        if (passThrough != null && !passThrough.isBlank()) {
            JsonNode val = ctx.resolve(passThrough);
            if (!val.isMissingNode()) {
                output = val.deepCopy();
            }
        }

        if (logErrors) {
            ctx.log().info("Try/Catch passed through " + (passThrough == null ? "trigger" : passThrough) + ".");
        }
        return NodeResult.success(output);
    }
}