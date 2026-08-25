package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Manual Trigger: the entry point for a user-initiated run. It has no work to do —
 * it simply forwards the trigger payload as its output so downstream nodes can read
 * {@code {{manual_trigger.*}}} or {@code {{trigger.*}}}.
 */
@Component
public class ManualTriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "manual_trigger";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        JsonNode payload = ctx.resolve("trigger");
        ctx.log().info("Manual trigger fired.");
        JsonNode output = payload.isMissingNode() ? ctx.mapper().createObjectNode() : payload.deepCopy();
        return NodeResult.success(output);
    }
}
