package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * File Trigger: forwards the trigger payload for file-related starts.
 */
@Component
public class FileTriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "file_trigger";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        JsonNode payload = ctx.resolve("trigger");
        ctx.log().info("File trigger fired.");
        JsonNode output = payload.isMissingNode() ? ctx.mapper().createObjectNode() : payload.deepCopy();
        return NodeResult.success(output);
    }
}