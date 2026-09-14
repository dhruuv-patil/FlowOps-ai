package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Loop: builds an array by applying a mapping N times. */
@Component
public class LoopExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "loop";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String countStr = ctx.configString("count");
        Object rawMapping = ctx.config().get("mapping");
        if (countStr == null || countStr.isBlank()) {
            return NodeResult.fail("Loop requires a 'count'.");
        }
        if (rawMapping == null) {
            return NodeResult.fail("Loop requires a 'mapping' object.");
        }

        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException nfe) {
            return NodeResult.fail("Loop 'count' must be an integer.");
        }
        if (count < 0 || count > 1000) {
            return NodeResult.fail("Loop 'count' must be between 0 and 1000.");
        }

        ArrayNode out = ctx.mapper().createArrayNode();
        for (int i = 0; i < count; i++) {
            ObjectNode loopVars = ctx.mapper().createObjectNode();
            loopVars.put("index", i);
            // We need to make loop vars available for interpolation
            // For now, just interpolate with existing variables and add loop index
            ObjectNode mapped = ctx.mapper().createObjectNode();
            if (rawMapping instanceof java.util.Map<?, ?> map) {
                for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    String template = String.valueOf(e.getValue());
                    // Replace {{loop.index}}
                    String interpolated = template.replace("{{loop.index}}", String.valueOf(i));
                    interpolated = ctx.interpolate(interpolated);
                    mapped.set(key, tryParse(ctx, interpolated));
                }
            }
            out.add(mapped);
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("items", out);
        output.put("count", count);
        ctx.log().info("Loop produced " + count + " iterations.");
        return NodeResult.success(output);
    }

    private JsonNode tryParse(NodeExecutionContext ctx, String text) {
        try {
            return ctx.mapper().readTree(text);
        } catch (Exception e) {
            return ctx.mapper().getNodeFactory().textNode(text);
        }
    }
}