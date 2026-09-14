package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Escalation: escalates the run to a channel with stepped urgency. */
@Component
public class EscalationExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "escalation";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String title = ctx.configString("title");
        String body = ctx.configString("body");
        String levelStr = ctx.configString("level");
        String channel = ctx.configString("channel");
        if (title == null || title.isBlank()) return NodeResult.fail("Escalation requires 'title'.");

        int level = 1;
        if (levelStr != null && !levelStr.isBlank()) {
            try { level = Integer.parseInt(levelStr); } catch (NumberFormatException ignored) {}
        }

        String interpolatedTitle = ctx.interpolate(title);
        String interpolatedBody = body != null ? ctx.interpolate(body) : "";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("title", interpolatedTitle);
        output.put("body", interpolatedBody);
        output.put("level", level);
        output.put("channel", channel != null ? channel : "none");
        output.put("escalatedAt", java.time.Instant.now().toString());

        ctx.log().warn("Escalation level " + level + ": " + interpolatedTitle);
        return NodeResult.success(output);
    }
}