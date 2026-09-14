package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** For Each: maps every array item through a template into an output array. */
@Component
public class ForEachExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "for_each";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String arrayPath = ctx.configString("array");
        Object rawMapping = ctx.config().get("mapping");
        String limitStr = ctx.configString("limit");

        if (arrayPath == null || arrayPath.isBlank()) {
            return NodeResult.fail("For Each requires an 'array' path.");
        }
        if (rawMapping == null) {
            return NodeResult.fail("For Each requires a 'mapping' object.");
        }

        JsonNode arrayNode = ctx.resolve(arrayPath);
        if (arrayNode.isMissingNode() || !arrayNode.isArray()) {
            return NodeResult.fail("For Each array path '" + arrayPath + "' not found or not an array.");
        }

        int limit = -1;
        if (limitStr != null && !limitStr.isBlank()) {
            try {
                limit = Integer.parseInt(limitStr);
            } catch (NumberFormatException ignored) {
            }
        }

        ArrayNode out = ctx.mapper().createArrayNode();
        int count = 0;
        for (JsonNode item : arrayNode) {
            if (limit >= 0 && count >= limit) {
                break;
            }
            // Build variable root with item
            ObjectNode itemRoot = ctx.mapper().createObjectNode();
            itemRoot.set("item", item);
            // Merge with existing variables (item takes precedence for {{item}} but we don't support that directly)
            // We'll just interpolate the mapping with full variables + item
            ObjectNode mapped = ctx.mapper().createObjectNode();
            if (rawMapping instanceof java.util.Map<?, ?> map) {
                for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    String template = String.valueOf(e.getValue());
                    String interpolated = ctx.interpolate(template); // uses full variable root
                    // Also replace {{item}} and {{item.field}} manually
                    if (item.isObject()) {
                        for (java.util.Iterator<String> it = item.fieldNames(); it.hasNext();) {
                            String field = it.next();
                            interpolated = interpolated.replace("{{item." + field + "}}", item.get(field).asText());
                        }
                        interpolated = interpolated.replace("{{item}}", item.toString());
                    }
                    mapped.set(key, tryParse(ctx, interpolated));
                }
            } else if (rawMapping instanceof String text) {
                // mapping is a JSON string
                try {
                    JsonNode parsed = ctx.mapper().readTree(text);
                    if (parsed.isObject()) {
                        for (java.util.Iterator<String> it = parsed.fieldNames(); it.hasNext();) {
                            String key = it.next();
                            JsonNode val = parsed.get(key);
                            if (val.isTextual()) {
                                String interpolated = ctx.interpolate(val.asText());
                                if (item.isObject()) {
                                    for (java.util.Iterator<String> it2 = item.fieldNames(); it2.hasNext();) {
                                        String field = it2.next();
                                        interpolated = interpolated.replace("{{item." + field + "}}", item.get(field).asText());
                                    }
                                    interpolated = interpolated.replace("{{item}}", item.toString());
                                }
                                mapped.set(key, tryParse(ctx, interpolated));
                            } else {
                                mapped.set(key, val);
                            }
                        }
                    }
                } catch (Exception e) {
                    // treat as template string
                    String interpolated = ctx.interpolate(text);
                    if (item.isObject()) {
                        for (java.util.Iterator<String> it = item.fieldNames(); it.hasNext();) {
                            String field = it.next();
                            interpolated = interpolated.replace("{{item." + field + "}}", item.get(field).asText());
                        }
                        interpolated = interpolated.replace("{{item}}", item.toString());
                    }
                    mapped.set("result", tryParse(ctx, interpolated));
                }
            }
            out.add(mapped);
            count++;
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("items", out);
        output.put("count", count);
        ctx.log().info("For Each mapped " + count + " items.");
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