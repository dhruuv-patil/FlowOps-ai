package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Email Trigger: forwards the trigger payload for email-received starts.
 */
@Component
public class EmailTriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "email_trigger";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        JsonNode payload = ctx.resolve("trigger");
        ctx.log().info("Email trigger fired.");
        JsonNode output = payload.isMissingNode() ? ctx.mapper().createObjectNode() : payload.deepCopy();
        return NodeResult.success(output);
    }
}