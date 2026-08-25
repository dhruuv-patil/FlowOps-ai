package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.ExpressionEvaluator;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Condition: interpolates its expression, evaluates it, and fires exactly one of
 * its two output ports — {@code true} or {@code false}. Only edges leaving the
 * fired port become active, so the untaken branch's nodes end up {@code SKIPPED}.
 */
@Component
public class ConditionExecutor implements NodeExecutor {

    private final ExpressionEvaluator evaluator;

    public ConditionExecutor(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String type() {
        return "condition";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String raw = ctx.configString("expression");
        if (raw == null || raw.isBlank()) {
            return NodeResult.fail("Condition has no expression configured.");
        }
        boolean result = evaluator.evaluate(raw);
        String handle = result ? "true" : "false";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("expression", raw);
        output.put("result", result);
        ctx.log().info("Condition evaluated to " + result + " → branch \"" + handle + "\".");
        return NodeResult.success(output, List.of(handle));
    }
}
