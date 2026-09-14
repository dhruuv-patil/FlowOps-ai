package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Schedule Trigger: forwards the trigger payload (the cron firing event) as output.
 * The scheduling is handled externally (via a scheduler bean that starts webhook runs);
 * this executor just passes through the payload when a run is started by the scheduler.
 */
@Component
public class ScheduleTriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "schedule_trigger";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        JsonNode payload = ctx.resolve("trigger");
        ctx.log().info("Schedule trigger fired.");
        JsonNode output = payload.isMissingNode() ? ctx.mapper().createObjectNode() : payload.deepCopy();
        return NodeResult.success(output);
    }
}