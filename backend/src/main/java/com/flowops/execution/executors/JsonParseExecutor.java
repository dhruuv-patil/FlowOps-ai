package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** JSON Parse: parses a JSON string into a value. */
@Component
public class JsonParseExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "json_parse";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        if (input == null || input.isBlank()) {
            return NodeResult.fail("JSON Parse requires an 'input' string.");
        }

        String interpolated = ctx.interpolate(input);
        JsonNode parsed;
        try {
            parsed = ctx.mapper().readTree(interpolated);
        } catch (Exception e) {
            return NodeResult.fail("JSON Parse: invalid JSON - " + e.getMessage());
        }

        ctx.log().info("JSON Parse succeeded.");
        return NodeResult.success(parsed);
    }
}