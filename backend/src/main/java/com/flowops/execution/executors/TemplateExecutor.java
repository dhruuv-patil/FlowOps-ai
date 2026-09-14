package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Template: renders a template with {{variable}} interpolation to text. */
@Component
public class TemplateExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "template";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String template = ctx.configString("template");
        if (template == null || template.isBlank()) {
            return NodeResult.fail("Template requires a 'template' string.");
        }

        boolean trimResult = Boolean.TRUE.equals(ctx.config().get("trimResult"));
        String result = ctx.interpolate(template);
        if (trimResult) {
            result = result.trim();
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("result", result);
        ctx.log().info("Template rendered " + result.length() + " characters.");
        return NodeResult.success(output);
    }
}