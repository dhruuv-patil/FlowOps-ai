package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.ExpressionEvaluator;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** While: iterates a mapping while a condition holds. */
@Component
public class WhileExecutor implements NodeExecutor {

    private final ExpressionEvaluator evaluator;

    public WhileExecutor(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String type() {
        return "while";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String condition = ctx.configString("condition");
        Object rawMapping = ctx.config().get("mapping");
        String maxIterStr = ctx.configString("maxIterations");

        if (condition == null || condition.isBlank()) {
            return NodeResult.fail("While requires a 'condition'.");
        }
        if (rawMapping == null) {
            return NodeResult.fail("While requires a 'mapping' object.");
        }

        int maxIter = 100;
        if (maxIterStr != null && !maxIterStr.isBlank()) {
            try {
                maxIter = Integer.parseInt(maxIterStr);
            } catch (NumberFormatException ignored) {
            }
        }
        if (maxIter > 1000) {
            maxIter = 1000;
        }

        ArrayNode out = ctx.mapper().createArrayNode();
        int count = 0;
        for (int i = 0; i < maxIter; i++) {
            // Build variable root with loop index
            ObjectNode loopRoot = ctx.mapper().createObjectNode();
            loopRoot.put("loop.index", i);
            // Evaluate condition against loop index + upstream variables
            // Interpolate condition with loop.index
            String condWithLoop = condition.replace("{{loop.index}}", String.valueOf(i));
            condWithLoop = ctx.interpolate(condWithLoop); // also replaces upstream vars
            boolean continueLoop = evaluator.evaluate(condWithLoop);
            if (!continueLoop) {
                break;
            }

            ObjectNode mapped = ctx.mapper().createObjectNode();
            if (rawMapping instanceof java.util.Map<?, ?> map) {
                for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    String template = String.valueOf(e.getValue());
                    String interpolated = template.replace("{{loop.index}}", String.valueOf(i));
                    interpolated = ctx.interpolate(interpolated);
                    mapped.set(key, tryParse(ctx, interpolated));
                }
            }
            out.add(mapped);
            count++;
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("items", out);
        output.put("count", count);
        output.put("stoppedAtMax", count >= maxIter);
        ctx.log().info("While executed " + count + " iterations.");
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