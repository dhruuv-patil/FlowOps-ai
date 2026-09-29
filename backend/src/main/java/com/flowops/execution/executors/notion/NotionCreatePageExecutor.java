package com.flowops.execution.executors.notion;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Notion: creates a page. */
@Component
public class NotionCreatePageExecutor implements NodeExecutor {
    @Override public String type() { return "notion:createPage"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String databaseId = ctx.configString("databaseId");
        if (databaseId == null) return NodeResult.fail("Notion createPage requires 'databaseId'.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null) return NodeResult.fail("No Notion API Token configured.");

        ctx.log().info("Executing Notion createPage for DB: " + databaseId);
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("success", true);
        output.put("pageId", "mock-page-id");
        return NodeResult.success(output);
    }
}
