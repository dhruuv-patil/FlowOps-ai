package com.flowops.execution.executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Transform: builds a new object from a mapping of output keys to {@code {{...}}}
 * expressions, so downstream nodes get exactly the shape they need. String values
 * are interpolated then coerced to JSON where they parse (so {@code "200"} becomes
 * a number, {@code "true"} a boolean); non-string values are carried through.
 */
@Component
public class TransformExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "transform";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawMapping = ctx.config().get("mapping");
        Map<String, Object> mapping;
        if (rawMapping instanceof Map<?, ?> map) {
            mapping = (Map<String, Object>) map;
        } else if (rawMapping instanceof String text && !text.isBlank()) {
            JsonNode parsed = tryParse(ctx, text);
            if (parsed == null || !parsed.isObject()) {
                return NodeResult.fail("Transform mapping must be a JSON object.");
            }
            mapping = ctx.mapper().convertValue(parsed, Map.class);
        } else {
            return NodeResult.fail("Transform has no mapping configured.");
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            output.set(entry.getKey(), coerce(ctx, entry.getValue()));
        }
        ctx.log().info("Transform produced " + output.size() + " field(s).");
        return NodeResult.success(output);
    }

    /** Interpolates strings and coerces them to JSON where possible; passes other values through. */
    private JsonNode coerce(NodeExecutionContext ctx, Object value) {
        if (value instanceof String template) {
            String interpolated = ctx.interpolate(template);
            JsonNode parsed = tryParse(ctx, interpolated);
            return parsed != null ? parsed : ctx.mapper().getNodeFactory().textNode(interpolated);
        }
        return ctx.mapper().valueToTree(value);
    }

    private JsonNode tryParse(NodeExecutionContext ctx, String text) {
        try {
            return ctx.mapper().readTree(text);
        } catch (JsonProcessingException notJson) {
            return null;
        }
    }
}
