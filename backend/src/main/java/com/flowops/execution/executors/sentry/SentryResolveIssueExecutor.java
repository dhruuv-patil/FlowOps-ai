package com.flowops.execution.executors.sentry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.provider.sentry.SentryClient;
import org.springframework.stereotype.Component;

/** Sentry: resolves an issue. */
@Component
public class SentryResolveIssueExecutor implements NodeExecutor {
    private final SentryClient client;

    public SentryResolveIssueExecutor(SentryClient client) {
        this.client = client;
    }

    @Override public String type() { return "sentry:resolveIssue"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String orgSlug = ctx.configString("orgSlug");
        String projectSlug = ctx.configString("projectSlug");
        String issueId = ctx.configString("issueId");

        if (orgSlug == null || projectSlug == null || issueId == null)
            return NodeResult.fail("Sentry resolveIssue requires 'orgSlug', 'projectSlug', and 'issueId'.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null) return NodeResult.fail("No Sentry API Token configured.");

        try {
            String path = "issues/" + issueId + "/";
            String payload = "{\"status\":\"resolved\"}";

            var response = client.execute(apiToken, "PUT", path, payload);

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() == 200 || response.statusCode() == 204);
            output.put("status", response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Sentry API error: " + e.getMessage());
        }
    }
}
