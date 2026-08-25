package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * AI Agent: a placeholder until the Python AI service arrives in M4. It does not
 * fabricate a completion — it succeeds with {@code configured: false} and an honest
 * note, and echoes the interpolated input so the rest of the run can proceed and be
 * inspected. Once M4 wires the AI service, this executor calls it with a real key.
 */
@Component
public class AiAgentExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "ai_agent";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("configured", false);
        output.put("note", "AI execution is available once an AI provider key is configured (M4).");
        output.put("input", input == null ? "" : input);

        ctx.log().warn("AI Agent is not configured yet — no completion was generated (arrives in M4).");
        return NodeResult.success(output);
    }
}
