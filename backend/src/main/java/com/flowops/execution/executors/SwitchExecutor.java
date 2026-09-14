package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Switch: routes based on field value against cases array. */
@Component
public class SwitchExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "switch";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String field = ctx.configString("field");
        if (field == null || field.isBlank()) {
            return NodeResult.fail("Switch requires a 'field' path.");
        }
        Object rawCases = ctx.config().get("cases");
        JsonNode casesNode = rawCases instanceof JsonNode ? (JsonNode) rawCases : null;
        if (casesNode == null || !casesNode.isArray()) {
            return NodeResult.fail("Switch requires a 'cases' array.");
        }

        JsonNode value = ctx.resolve(field);
        if (value.isMissingNode() || value.isNull()) {
            return NodeResult.fail("Switch field '" + field + "' not found in variables.");
        }
        String strValue = value.isTextual() ? value.asText() : value.toString();

        String handle = "default";
        for (int i = 0; i < casesNode.size() && i < 4; i++) {
            String caseVal = casesNode.get(i).asText();
            if (strValue.equals(caseVal)) {
                handle = "case" + (i + 1);
                break;
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("matched", handle);
        output.put("value", strValue);
        ctx.log().info("Switch matched '" + strValue + "' → branch \"" + handle + "\".");
        return NodeResult.success(output, java.util.List.of(handle));
    }
}