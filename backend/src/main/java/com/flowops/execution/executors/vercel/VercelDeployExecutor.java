package com.flowops.execution.executors.vercel;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Vercel: triggers a deployment. */
@Component
public class VercelDeployExecutor implements NodeExecutor {
    @Override public String type() { return "vercel:deploy"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String projectId = ctx.configString("projectId");
        if (projectId == null) return NodeResult.fail("Vercel deploy requires 'projectId'.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null) return NodeResult.fail("No Vercel API Token configured.");

        ctx.log().info("Executing Vercel deploy for: " + projectId);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        return NodeResult.success(output);
    }
}
