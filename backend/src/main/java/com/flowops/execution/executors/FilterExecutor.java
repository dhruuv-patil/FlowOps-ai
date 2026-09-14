package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.ExpressionEvaluator;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Filter: keeps or drops array items matching a predicate. */
@Component
public class FilterExecutor implements NodeExecutor {

    private final ExpressionEvaluator evaluator;

    public FilterExecutor(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String type() {
        return "filter";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String arrayPath = ctx.configString("array");
        String keepWhere = ctx.configString("keepWhere");
        String mode = ctx.configString("mode");
        if (arrayPath == null || arrayPath.isBlank()) {
            return NodeResult.fail("Filter requires an 'array' path.");
        }
        if (keepWhere == null || keepWhere.isBlank()) {
            return NodeResult.fail("Filter requires a 'keepWhere' predicate.");
        }
        boolean keep = !"drop".equals(mode);

        JsonNode arrayNode = ctx.resolve(arrayPath);
        if (arrayNode.isMissingNode() || !arrayNode.isArray()) {
            return NodeResult.fail("Filter array path '" + arrayPath + "' not found or not an array.");
        }

        ArrayNode out = ctx.mapper().createArrayNode();
        for (JsonNode item : arrayNode) {
            String expr = keepWhere;
            // Replace {{item}} with the current item
            if (item.isObject()) {
                expr = expr.replace("{{item}}", item.toString());
                // Also support {{item.field}} by resolving against the item
                // Simple approach: interpolate with item as root
            }
            // For simplicity, just pass the whole item context to evaluator
            // We'll build a variable root with "item" key
            com.fasterxml.jackson.databind.node.ObjectNode itemRoot = ctx.mapper().createObjectNode();
            itemRoot.set("item", item);
            // Evaluate against item + upstream variables
            // Note: we just interpolate the predicate with item's fields available as {{item.field}}
            // For now, simple interpolation of the whole expression
            String resolvedExpr = ctx.interpolate(keepWhere); // uses full variable root
            // Try to extract item fields
            if (item.isObject()) {
                for (java.util.Iterator<String> it = item.fieldNames(); it.hasNext();) {
                    String key = it.next();
                    resolvedExpr = resolvedExpr.replace("{{item." + key + "}}", item.get(key).asText());
                }
            }
            boolean match = evaluator.evaluate(resolvedExpr);
            if ((keep && match) || (!keep && !match)) {
                out.add(item);
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("items", out);
        output.put("count", out.size());
        ctx.log().info("Filter kept " + out.size() + " of " + arrayNode.size() + " items (mode=" + (keep ? "keep" : "drop") + ").");
        return NodeResult.success(output);
    }
}