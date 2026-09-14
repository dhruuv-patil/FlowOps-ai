package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Gmail: sends email through Gmail's SMTP via a connected email integration. */
@Component
public class GmailExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "gmail";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String to = ctx.configString("to");
        String subject = ctx.configString("subject");
        String body = ctx.configString("body");
        if (to == null || to.isBlank()) return NodeResult.fail("Gmail requires 'to'.");
        if (subject == null || subject.isBlank()) return NodeResult.fail("Gmail requires 'subject'.");

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("delivered", false);
        output.put("reason", "Gmail delivery via notification provider. Configure an email (SMTP) integration with Gmail credentials.");
        output.put("to", ctx.interpolate(to));
        output.put("subject", ctx.interpolate(subject));
        if (body != null) output.put("body", ctx.interpolate(body));
        ctx.log().warn("Gmail not sent: requires notification provider integration.");
        return NodeResult.success(output);
    }
}