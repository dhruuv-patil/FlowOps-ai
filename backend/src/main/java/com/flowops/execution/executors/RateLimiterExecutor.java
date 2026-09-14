package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Rate Limiter: assesses rate against budget, branches ok/exceeded. */
@Component
public class RateLimiterExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "rate_limiter";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String ratePath = ctx.configString("ratePath");
        String maxRateStr = ctx.configString("maxRate");
        if (ratePath == null || ratePath.isBlank()) {
            return NodeResult.fail("Rate Limiter requires a 'ratePath'.");
        }
        if (maxRateStr == null || maxRateStr.isBlank()) {
            return NodeResult.fail("Rate Limiter requires a 'maxRate'.");
        }

        double maxRate;
        try {
            maxRate = Double.parseDouble(maxRateStr);
        } catch (NumberFormatException nfe) {
            return NodeResult.fail("Rate Limiter 'maxRate' must be a number.");
        }

        JsonNode rateNode = ctx.resolve(ratePath);
        if (rateNode.isMissingNode() || rateNode.isNull()) {
            return NodeResult.fail("Rate Limiter rate path '" + ratePath + "' not found.");
        }

        double rate;
        if (rateNode.isNumber()) {
            rate = rateNode.asDouble();
        } else {
            try {
                rate = Double.parseDouble(rateNode.asText());
            } catch (NumberFormatException nfe) {
                return NodeResult.fail("Rate Limiter rate at '" + ratePath + "' is not a number.");
            }
        }

        boolean ok = rate <= maxRate;
        String handle = ok ? "ok" : "exceeded";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("rate", rate);
        output.put("maxRate", maxRate);
        output.put("status", handle);

        ctx.log().info("Rate Limiter: rate=" + rate + " max=" + maxRate + " → " + handle + ".");
        return NodeResult.success(output, java.util.List.of(handle));
    }
}