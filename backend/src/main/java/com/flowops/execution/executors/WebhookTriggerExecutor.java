package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Webhook Trigger: the entry point for a run started by an inbound HTTP request.
 * The request body/headers captured at ingress are the trigger payload, which this
 * node forwards as its output. (Secure webhook ingress itself lands in M5; this
 * executor already runs correctly when a run is started with a webhook payload.)
 */
@Component
public class WebhookTriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "webhook_trigger";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        JsonNode payload = ctx.resolve("trigger");
        ctx.log().info("Webhook trigger fired.");
        JsonNode output = payload.isMissingNode() ? ctx.mapper().createObjectNode() : payload.deepCopy();
        return NodeResult.success(output);
    }
}
