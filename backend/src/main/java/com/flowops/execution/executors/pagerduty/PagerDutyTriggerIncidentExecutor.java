package com.flowops.execution.executors.pagerduty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.provider.pagerduty.PagerDutyClient;
import org.springframework.stereotype.Component;

/** PagerDuty: triggers an incident. */
@Component
public class PagerDutyTriggerIncidentExecutor implements NodeExecutor {
    private final PagerDutyClient client;

    public PagerDutyTriggerIncidentExecutor(PagerDutyClient client) {
        this.client = client;
    }

    @Override public String type() { return "pagerduty:triggerIncident"; }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String serviceId = ctx.configString("serviceId");
        String title = ctx.configString("title");
        if (serviceId == null || title == null) return NodeResult.fail("PagerDuty triggerIncident requires 'serviceId' and 'title'.");

        String apiToken = ctx.secret("apiToken");
        if (apiToken == null) return NodeResult.fail("No PagerDuty API Token configured.");

        try {
            String payload = ctx.mapper().writeValueAsString(java.util.Map.of(
                "incident", java.util.Map.of(
                    "type", "incident",
                    "title", ctx.interpolate(title),
                    "service", java.util.Map.of("id", serviceId, "type", "service_reference")
                )
            ));

            var response = client.execute(apiToken, "POST", "incidents", payload);

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("success", response.statusCode() == 201);
            output.put("status", response.statusCode());
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("PagerDuty API error: " + e.getMessage());
        }
    }
}
