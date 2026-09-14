package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Router: routes based on regex patterns against routes array. */
@Component
public class RouterExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "router";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        if (input == null || input.isBlank()) {
            return NodeResult.fail("Router requires an 'input' value or path.");
        }
        String interpolatedInput = ctx.interpolate(input);

        Object rawRoutes = ctx.config().get("routes");
        JsonNode routesNode = rawRoutes instanceof JsonNode ? (JsonNode) rawRoutes : null;
        if (routesNode == null || !routesNode.isArray()) {
            return NodeResult.fail("Router requires a 'routes' array of regex patterns.");
        }

        String handle = "default";
        for (int i = 0; i < routesNode.size() && i < 6; i++) {
            String pattern = routesNode.get(i).asText();
            try {
                if (Pattern.compile(pattern).matcher(interpolatedInput).matches()) {
                    handle = "route" + (i + 1);
                    break;
                }
            } catch (Exception badPattern) {
                // Skip invalid pattern
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("matched", handle);
        output.put("input", interpolatedInput);
        ctx.log().info("Router matched → branch \"" + handle + "\".");
        return NodeResult.success(output, java.util.List.of(handle));
    }
}