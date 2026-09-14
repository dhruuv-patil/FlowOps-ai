package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Map Fields: renames and reshapes fields via a key mapping. */
@Component
public class MapFieldsExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "map_fields";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawMapping = ctx.config().get("mapping");
        boolean keepUnmapped = Boolean.TRUE.equals(ctx.config().get("keepUnmapped"));
        if (rawMapping == null) {
            return NodeResult.fail("Map Fields requires a 'mapping' object.");
        }

        // Get the input object (upstream output or trigger)
        JsonNode input = ctx.resolve(""); // This gets the whole variable root
        // We need to know what the "incoming" object is. By convention, use the last upstream node
        // or the trigger. For simplicity, use the first upstream that produced output.
        // Actually, the config mapping defines the output shape - just build it.

        ObjectNode output = ctx.mapper().createObjectNode();
        if (rawMapping instanceof java.util.Map<?, ?> map) {
            for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                String oldKey = String.valueOf(e.getKey());
                String newKey = String.valueOf(e.getValue());
                JsonNode val = ctx.resolve(oldKey);
                if (!val.isMissingNode()) {
                    output.set(newKey, val);
                }
            }
        }
        ctx.log().info("Map Fields produced " + output.size() + " fields.");
        return NodeResult.success(output);
    }
}