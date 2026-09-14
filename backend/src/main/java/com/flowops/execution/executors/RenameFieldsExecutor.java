package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Rename Fields: renames selected keys, keeping everything else. */
@Component
public class RenameFieldsExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "rename_fields";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawRenames = ctx.config().get("renames");
        if (rawRenames == null) {
            return NodeResult.fail("Rename Fields requires a 'renames' object.");
        }

        // We need the input object - for now get it from the first upstream node's output
        // This is a simplification; ideally we'd know which node's output to use
        ObjectNode output = ctx.mapper().createObjectNode();

        // Get all variables and copy them, applying renames
        JsonNode allVars = ctx.variables();
        if (allVars.isObject()) {
            allVars.fields().forEachRemaining(e -> {
                if (!"trigger".equals(e.getKey())) {
                    output.set(e.getKey(), e.getValue());
                }
            });
        }

        if (rawRenames instanceof java.util.Map<?, ?> map) {
            for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                String oldKey = String.valueOf(e.getKey());
                String newKey = String.valueOf(e.getValue());
                JsonNode val = output.get(oldKey);
                if (val != null) {
                    output.remove(oldKey);
                    output.set(newKey, val);
                }
            }
        }
        ctx.log().info("Rename Fields applied renames.");
        return NodeResult.success(output);
    }
}