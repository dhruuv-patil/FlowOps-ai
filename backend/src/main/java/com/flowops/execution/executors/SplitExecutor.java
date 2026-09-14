package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Split: splits a string by delimiter or chunks an array. */
@Component
public class SplitExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "split";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        String mode = ctx.configString("mode");
        if (input == null || input.isBlank()) {
            return NodeResult.fail("Split requires an 'input' string or path.");
        }
        String interpolated = ctx.interpolate(input);
        String delimiter = ctx.configString("delimiter");
        String chunkSizeStr = ctx.configString("chunkSize");

        ArrayNode out = ctx.mapper().createArrayNode();

        if ("chunks".equals(mode)) {
            int chunkSize = 10;
            try {
                chunkSize = Integer.parseInt(chunkSizeStr == null ? "10" : chunkSizeStr);
            } catch (NumberFormatException ignored) {
            }
            if (interpolated.startsWith("[")) {
                try {
                    JsonNode arr = ctx.mapper().readTree(interpolated);
                    if (arr.isArray()) {
                        for (int i = 0; i < arr.size(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, arr.size());
                            ArrayNode chunk = ctx.mapper().createArrayNode();
                            for (int j = i; j < end; j++) {
                                chunk.add(arr.get(j));
                            }
                            out.add(chunk);
                        }
                    }
                } catch (Exception e) {
                    return NodeResult.fail("Split chunks mode: input is not a valid JSON array.");
                }
            } else {
                // Treat as string, chunk by characters
                for (int i = 0; i < interpolated.length(); i += chunkSize) {
                    out.add(ctx.mapper().getNodeFactory().textNode(interpolated.substring(i, Math.min(i + chunkSize, interpolated.length()))));
                }
            }
        } else {
            // delimiter mode
            String delim = delimiter == null || delimiter.isBlank() ? "," : delimiter;
            String[] parts = interpolated.split(java.util.regex.Pattern.quote(delim));
            for (String part : parts) {
                out.add(ctx.mapper().getNodeFactory().textNode(part));
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("parts", out);
        output.put("count", out.size());
        ctx.log().info("Split produced " + out.size() + " parts (mode=" + (mode == null ? "delimiter" : mode) + ").");
        return NodeResult.success(output);
    }
}