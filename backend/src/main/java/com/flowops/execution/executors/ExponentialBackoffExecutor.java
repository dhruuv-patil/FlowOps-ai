package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Exponential Backoff: waits an exponentially-growing delay. */
@Component
public class ExponentialBackoffExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "exponential_backoff";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws InterruptedException {
        String initialStr = ctx.configString("initialSeconds");
        String factorStr = ctx.configString("factor");
        String attemptStr = ctx.configString("attempt");
        String sleepStr = ctx.configString("sleep");

        if (initialStr == null || initialStr.isBlank()) {
            return NodeResult.fail("Exponential Backoff requires 'initialSeconds'.");
        }

        double initial;
        try {
            initial = Double.parseDouble(initialStr);
        } catch (NumberFormatException nfe) {
            return NodeResult.fail("Exponential Backoff 'initialSeconds' must be a number.");
        }

        double factor = 2.0;
        if (factorStr != null && !factorStr.isBlank()) {
            try {
                factor = Double.parseDouble(factorStr);
            } catch (NumberFormatException ignored) {
            }
        }

        int attempt = 1;
        if (attemptStr != null && !attemptStr.isBlank()) {
            try {
                attempt = Integer.parseInt(attemptStr);
            } catch (NumberFormatException ignored) {
            }
        }
        if (attempt < 1) {
            attempt = 1;
        }

        boolean sleep = Boolean.TRUE.equals(ctx.config().get("sleep"));

        double wait = initial * Math.pow(factor, attempt - 1);
        double cap = ctx.properties().maxDelaySeconds();
        if (wait > cap) {
            wait = cap;
        }

        if (sleep && wait > 0) {
            Thread.sleep((long) (wait * 1000));
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("initialSeconds", initial);
        output.put("factor", factor);
        output.put("attempt", attempt);
        output.put("waitSeconds", wait);
        output.put("slept", sleep && wait > 0);
        ctx.log().info("Exponential Backoff: attempt=" + attempt + " wait=" + wait + "s" + (sleep ? " (slept)" : " (reported)") + ".");
        return NodeResult.success(output);
    }
}