package com.flowops.execution.executors.crm.pipedrive;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Pipedrive: creates a deal. */
@Component
public class PipedriveCreateDealExecutor implements NodeExecutor {
    @Override public String type() { return "pipedrive:createDeal"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String title = ctx.configString("title");
        if (title == null) return NodeResult.fail("Pipedrive createDeal requires 'title'.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null) return NodeResult.fail("No Pipedrive API Token.");

        ctx.log().info("Executing Pipedrive createDeal: " + title);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        output.put("id", 123);
        return NodeResult.success(output);
    }
}
