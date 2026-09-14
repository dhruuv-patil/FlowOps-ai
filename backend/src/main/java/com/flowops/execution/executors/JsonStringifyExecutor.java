package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** JSON Stringify: serializes a value to a JSON string. */
@Component
public class JsonStringifyExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "json_stringify";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        if (input == null || input.isBlank()) {
            return NodeResult.fail("JSON Stringify requires an 'input' value.");
        }

        JsonNode val = ctx.resolve(input);
        if (val.isMissingNode() || val.isNull()) {
            return NodeResult.fail("JSON Stringify input path '" + input + "' not found.");
        }

        boolean pretty = Boolean.TRUE.equals(ctx.config().get("pretty"));
        String stringified;
        try {
            stringified = pretty ? ctx.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(val)
                                 : ctx.mapper().writeValueAsString(val);
        } catch (Exception e) {
            return NodeResult.fail("JSON Stringify failed: " + e.getMessage());
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("json", stringified);
        ctx.log().info("JSON Stringify produced " + stringified.length() + " characters.");
        return NodeResult.success(output);
    }
}