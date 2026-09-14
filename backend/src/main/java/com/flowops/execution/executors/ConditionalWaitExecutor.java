package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.ExpressionEvaluator;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Conditional Wait: pauses until an expression becomes true. */
@Component
public class ConditionalWaitExecutor implements NodeExecutor {

    private final ExpressionEvaluator evaluator;

    public ConditionalWaitExecutor(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String type() {
        return "conditional_wait";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws InterruptedException {
        String expression = ctx.configString("expression");
        if (expression == null || expression.isBlank()) {
            return NodeResult.fail("Conditional Wait requires an 'expression'.");
        }

        String pollIntervalStr = ctx.configString("pollIntervalSeconds");
        String timeoutStr = ctx.configString("timeoutSeconds");

        double pollInterval = 5.0;
        if (pollIntervalStr != null && !pollIntervalStr.isBlank()) {
            try {
                pollInterval = Double.parseDouble(pollIntervalStr);
            } catch (NumberFormatException ignored) {
            }
        }
        if (pollInterval < 0.1) {
            pollInterval = 0.1;
        }

        double timeout = 300.0;
        if (timeoutStr != null && !timeoutStr.isBlank()) {
            try {
                timeout = Double.parseDouble(timeoutStr);
            } catch (NumberFormatException ignored) {
            }
        }
        if (timeout < 1) {
            timeout = 1;
        }

        double cap = ctx.properties().maxDelaySeconds();
        if (timeout > cap) {
            timeout = cap;
        }

        long deadline = System.currentTimeMillis() + (long) (timeout * 1000);
        int polls = 0;
        boolean satisfied = false;

        while (System.currentTimeMillis() < deadline) {
            polls++;
            String interp = ctx.interpolate(expression);
            if (evaluator.evaluate(interp)) {
                satisfied = true;
                break;
            }
            long sleepMs = (long) (pollInterval * 1000);
            if (System.currentTimeMillis() + sleepMs > deadline) {
                break;
            }
            Thread.sleep(sleepMs);
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("satisfied", satisfied);
        output.put("polls", polls);
        output.put("timeoutSeconds", timeout);
        ctx.log().info("Conditional Wait " + (satisfied ? "satisfied" : "timed out") + " after " + polls + " polls.");
        return NodeResult.success(output);
    }
}