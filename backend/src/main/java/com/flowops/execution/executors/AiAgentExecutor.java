package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.ai.AiServiceClient;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * AI Agent: calls the Python AI service to produce a real completion.
 *
 * <p>Honours the NO-FAKE-FUNCTIONALITY mandate at two points. If the node has no
 * instructions and references no saved agent, there is nothing to run — it succeeds
 * with {@code configured:false} and an honest note (the run continues, mirroring
 * {@code NotificationExecutor}). If the AI service itself has no provider key, the
 * completion comes back {@code configured:false} and this executor surfaces that
 * verbatim rather than inventing output. A real completion is returned only when the
 * service actually produced one.
 *
 * <p>The executor stays a pure function of (config, variables): the saved-agent
 * reference was already resolved into the effective config at the engine boundary,
 * so no database access happens here. A transport failure raises out of
 * {@link AiServiceClient} and the engine turns it into a retryable node failure.
 */
@Component
public class AiAgentExecutor implements NodeExecutor {

    private final AiServiceClient aiService;

    public AiAgentExecutor(AiServiceClient aiService) {
        this.aiService = aiService;
    }

    @Override
    public String type() {
        return "ai_agent";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String instructions = ctx.configString("instructions");
        String input = ctx.configString("input");
        String effectiveInput = input == null ? "" : input;

        if (instructions == null || instructions.isBlank()) {
            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("configured", false);
            output.put("note", "This AI Agent node has no instructions and no saved agent "
                    + "selected — nothing to run.");
            output.put("output", "");
            ctx.log().warn("AI Agent has no instructions or saved agent — no completion generated.");
            return NodeResult.success(output);
        }

        String model = ctx.configString("model");
        List<String> tools = ctx.configStringList("tools");

        AiServiceClient.AgentRun run = aiService.runAgent(instructions, model, tools, effectiveInput);

        ObjectNode output = ctx.mapper().createObjectNode();
        if (!run.configured()) {
            output.put("configured", false);
            output.put("note", "AI is not configured. Set an AI provider key on the AI service "
                    + "to enable this node.");
            output.put("output", "");
            ctx.log().warn("AI Agent ran but AI is not configured — no completion generated.");
            return NodeResult.success(output);
        }

        output.put("configured", true);
        output.put("output", run.output() == null ? "" : run.output());
        if (run.model() != null && !run.model().isBlank()) {
            output.put("model", run.model());
        }
        ArrayNode calls = output.putArray("toolCalls");
        for (AiServiceClient.AgentToolCall call : run.toolCalls()) {
            ObjectNode entry = calls.addObject();
            entry.put("name", call.name());
            entry.set("arguments", call.arguments() == null ? ctx.mapper().nullNode() : call.arguments());
            entry.put("result", call.result());
        }

        int toolCount = run.toolCalls().size();
        ctx.log().info(toolCount == 0
                ? "AI Agent produced a completion."
                : "AI Agent produced a completion using " + toolCount + " tool call(s).");
        return NodeResult.success(output);
    }
}
