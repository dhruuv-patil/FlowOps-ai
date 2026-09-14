package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** SLA Monitor: assesses success rate and latency against SLA. */
@Component
public class SlaMonitorExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "sla_monitor";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String successRatePath = ctx.configString("successRatePath");
        String latencyPath = ctx.configString("latencyPath");
        String minSuccessRateStr = ctx.configString("minSuccessRate");
        String maxLatencyStr = ctx.configString("maxLatencyMs");

        if (successRatePath == null || successRatePath.isBlank()) return NodeResult.fail("SLA Monitor requires 'successRatePath'.");
        if (latencyPath == null || latencyPath.isBlank()) return NodeResult.fail("SLA Monitor requires 'latencyPath'.");
        if (minSuccessRateStr == null || minSuccessRateStr.isBlank()) return NodeResult.fail("SLA Monitor requires 'minSuccessRate'.");
        if (maxLatencyStr == null || maxLatencyStr.isBlank()) return NodeResult.fail("SLA Monitor requires 'maxLatencyMs'.");

        double minSuccessRate, maxLatencyMs;
        try {
            minSuccessRate = Double.parseDouble(minSuccessRateStr);
            maxLatencyMs = Double.parseDouble(maxLatencyStr);
        } catch (NumberFormatException e) {
            return NodeResult.fail("SLA Monitor thresholds must be numbers.");
        }

        JsonNode successRateNode = ctx.resolve(successRatePath);
        JsonNode latencyNode = ctx.resolve(latencyPath);

        if (successRateNode.isMissingNode() || latencyNode.isMissingNode()) {
            return NodeResult.fail("SLA Monitor: one or both metric paths not found.");
        }

        double successRate = successRateNode.isNumber() ? successRateNode.asDouble() : 0;
        double latency = latencyNode.isNumber() ? latencyNode.asDouble() : 0;

        boolean inSla = successRate >= minSuccessRate && latency <= maxLatencyMs;
        String handle = inSla ? "inSla" : "breached";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("successRate", successRate);
        output.put("minSuccessRate", minSuccessRate);
        output.put("latencyMs", latency);
        output.put("maxLatencyMs", maxLatencyMs);
        output.put("inSla", inSla);

        ctx.log().info("SLA Monitor: successRate=" + successRate + " latency=" + latency + "ms → " + handle);
        return NodeResult.success(output, java.util.List.of(handle));
    }
}