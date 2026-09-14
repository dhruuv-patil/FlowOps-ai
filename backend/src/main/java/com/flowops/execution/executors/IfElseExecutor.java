package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.ExpressionEvaluator;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** If/Else: evaluates conditions and branches true/false. */
@Component
public class IfElseExecutor implements NodeExecutor {

    private final ExpressionEvaluator evaluator;

    public IfElseExecutor(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String type() {
        return "if_else";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawConditions = ctx.config().get("conditions");
        String singleExpr = ctx.configString("expression");
        boolean result = false;

        if (rawConditions instanceof Iterable<?> list) {
            for (Object expr : list) {
                if (expr != null) {
                    String interp = ctx.interpolate(expr.toString());
                    if (evaluator.evaluate(interp)) {
                        result = true;
                        break;
                    }
                }
            }
        } else if (singleExpr != null && !singleExpr.isBlank()) {
            result = evaluator.evaluate(ctx.interpolate(singleExpr));
        } else {
            return NodeResult.fail("If/Else requires either 'conditions' array or 'expression'.");
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("result", result);
        ctx.log().info("If/Else evaluated to " + result + " → branch \"" + (result ? "true" : "false") + "\".");
        return NodeResult.success(output, java.util.List.of(result ? "true" : "false"));
    }
}