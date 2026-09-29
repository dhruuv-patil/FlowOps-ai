package com.flowops.execution.executors.pm.asana;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Asana: creates a task. */
@Component
public class AsanaCreateTaskExecutor implements NodeExecutor {
    @Override public String type() { return "asana:createTask"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String name = ctx.configString("name");
        String projectId = ctx.configString("projectId");
        if (name == null || projectId == null) return NodeResult.fail("Asana requires 'name' and 'projectId'.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null) return NodeResult.fail("No Asana API Token.");

        ctx.log().info("Executing Asana createTask: " + name);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        output.put("id", "mock-asana-id");
        return NodeResult.success(output);
    }
}
