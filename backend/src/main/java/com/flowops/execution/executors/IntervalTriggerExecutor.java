package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Interval Trigger: forwards the trigger payload as output. The scheduling is
 * handled externally; this executor just passes through the payload.
 */
@Component
public class IntervalTriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "interval_trigger";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        JsonNode payload = ctx.resolve("trigger");
        ctx.log().info("Interval trigger fired.");
        JsonNode output = payload.isMissingNode() ? ctx.mapper().createObjectNode() : payload.deepCopy();
        return NodeResult.success(output);
    }
}