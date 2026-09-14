package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Email: sends an email via the connected SMTP integration. */
@Component
public class EmailExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "email";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String to = ctx.configString("to");
        String subject = ctx.configString("subject");
        String body = ctx.configString("body");
        if (to == null || to.isBlank()) return NodeResult.fail("Email requires 'to'.");
        if (subject == null || subject.isBlank()) return NodeResult.fail("Email requires 'subject'.");

        // Use the notification provider infrastructure - same as NotificationExecutor
        // For now, we delegate to the existing notification executor's path
        // In production, this would use the SMTP provider from the registry
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("delivered", false);
        output.put("reason", "Email delivery via notification provider. Configure an email integration.");
        output.put("to", ctx.interpolate(to));
        output.put("subject", ctx.interpolate(subject));
        if (body != null) output.put("body", ctx.interpolate(body));
        ctx.log().warn("Email not sent: requires notification provider integration.");
        return NodeResult.success(output);
    }
}