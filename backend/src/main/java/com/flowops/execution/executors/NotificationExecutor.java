package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Notification: renders the message it <em>would</em> send through the selected
 * channel. No provider is wired yet (integrations land in M5), so this node is
 * honest about it — it never fakes a delivery. It succeeds so the run can proceed,
 * with {@code delivered: false} and a clear reason, and logs a warning.
 */
@Component
public class NotificationExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "notification";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String channel = ctx.configString("channel");
        String message = ctx.configString("message");
        if (channel == null || channel.isBlank()) {
            return NodeResult.fail("Notification has no channel configured.");
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("channel", channel);
        output.put("message", message == null ? "" : message);
        output.put("delivered", false);
        output.put("reason", "No " + channel + " provider is connected — configure an integration to deliver.");

        ctx.log().warn("Notification not sent: no " + channel + " provider configured. Message was rendered only.");
        return NodeResult.success(output);
    }
}
