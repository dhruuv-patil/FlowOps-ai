package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Set Fields: adds/overrides fields on the incoming object. */
@Component
public class SetFieldsExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "set_fields";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawValues = ctx.config().get("values");
        if (rawValues == null) {
            return NodeResult.fail("Set Fields requires a 'values' object.");
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        if (rawValues instanceof java.util.Map<?, ?> map) {
            for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                String template = String.valueOf(e.getValue());
                String interpolated = ctx.interpolate(template);
                output.set(key, tryParse(ctx, interpolated));
            }
        }
        ctx.log().info("Set Fields added/updated " + output.size() + " fields.");
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