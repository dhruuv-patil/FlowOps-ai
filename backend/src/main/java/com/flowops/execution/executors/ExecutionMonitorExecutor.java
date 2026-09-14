package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Execution Monitor: reads an execution status from variables and reports it. */
@Component
public class ExecutionMonitorExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "execution_monitor";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String statusPath = ctx.configString("statusPath");
        String durationPath = ctx.configString("durationPath");

        ObjectNode output = ctx.mapper().createObjectNode();

        if (statusPath != null && !statusPath.isBlank()) {
            com.fasterxml.jackson.databind.JsonNode statusNode = ctx.resolve(statusPath);
            if (!statusNode.isMissingNode()) {
                output.set("status", statusNode);
            }
        }

        if (durationPath != null && !durationPath.isBlank()) {
            com.fasterxml.jackson.databind.JsonNode durationNode = ctx.resolve(durationPath);
            if (!durationNode.isMissingNode()) {
                output.set("durationMs", durationNode);
            }
        }

        ctx.log().info("Execution Monitor reported status.");
        return NodeResult.success(output);
    }
}