package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Remove Fields: removes selected keys from the incoming object. */
@Component
public class RemoveFieldsExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "remove_fields";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawFields = ctx.config().get("fields");
        if (rawFields == null) {
            return NodeResult.fail("Remove Fields requires a 'fields' array.");
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        JsonNode allVars = ctx.variables();
        if (allVars.isObject()) {
            allVars.fields().forEachRemaining(e -> {
                if (!"trigger".equals(e.getKey())) {
                    output.set(e.getKey(), e.getValue());
                }
            });
        }

        int removed = 0;
        if (rawFields instanceof java.util.List<?> list) {
            for (Object f : list) {
                String key = String.valueOf(f);
                if (output.remove(key) != null) {
                    removed++;
                }
            }
        }
        ctx.log().info("Remove Fields removed " + removed + " fields.");
        return NodeResult.success(output);
    }
}