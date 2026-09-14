package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Retry: records retry policy hint and passes through. */
@Component
public class RetryExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "retry";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String maxAttemptsStr = ctx.configString("maxAttempts");
        String backoffStr = ctx.configString("backoffSeconds");

        ObjectNode output = ctx.mapper().createObjectNode();
        if (maxAttemptsStr != null && !maxAttemptsStr.isBlank()) {
            try {
                output.put("maxAttempts", Integer.parseInt(maxAttemptsStr));
            } catch (NumberFormatException ignored) {
            }
        }
        if (backoffStr != null && !backoffStr.isBlank()) {
            try {
                output.put("backoffSeconds", Double.parseDouble(backoffStr));
            } catch (NumberFormatException ignored) {
            }
        }
        ctx.log().info("Retry policy recorded.");
        return NodeResult.success(output);
    }
}