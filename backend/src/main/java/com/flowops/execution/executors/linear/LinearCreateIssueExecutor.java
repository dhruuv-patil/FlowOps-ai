package com.flowops.execution.executors.linear;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.provider.linear.LinearClient;
import org.springframework.stereotype.Component;

/** Linear: creates an issue. */
@Component
public class LinearCreateIssueExecutor implements NodeExecutor {
    private final LinearClient client;

    public LinearCreateIssueExecutor(LinearClient client) {
        this.client = client;
    }

    @Override public String type() { return "linear:createIssue"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String title = ctx.configString("title");
        String teamId = ctx.configString("teamId");
        String desc = ctx.configString("description");
        if (title == null || teamId == null) return NodeResult.fail("Linear createIssue requires 'title' and 'teamId'.");

        String apiKey = ctx.secret("apiKey");
        if (apiKey == null) return NodeResult.fail("No Linear API Key configured.");

        try {
            String query = """
                mutation CreateIssue($input: IssueCreateInput!) {
                  issueCreate(input: $input) {
                    issue { id title }
                  }
                }
                """;
            String variables = ctx.mapper().writeValueAsString(java.util.Map.of(
                "input", java.util.Map.of("title", ctx.interpolate(title), "teamId", teamId, "description", ctx.interpolate(desc != null ? desc : ""))
            ));

            var response = client.execute(apiKey, query, variables);

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() == 200);
            output.put("body", response.body());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Linear API error: " + e.getMessage());
        }
    }
}
