package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Base executor for single-turn AI completion nodes.
 *
 * <p>Subclasses define their {@code systemPrompt} and {@code userPrompt} (which may
 * reference {{variables}}). The executor interpolates them, calls the AI service
 * via {@link AiCompleter}, and returns the model's text as the node output.
 * When the AI service is not configured the result is {@code configured:false}
 * with an honest note — no fake completion is ever fabricated.
 */
public abstract class AiCompletionExecutor implements NodeExecutor {

    private final AiCompleter aiCompleter;
    private final boolean jsonMode;

    protected AiCompletionExecutor(AiCompleter aiCompleter, boolean jsonMode) {
        this.aiCompleter = aiCompleter;
        this.jsonMode = jsonMode;
    }

    /** The system prompt (may be empty). */
    protected abstract String systemPrompt(NodeExecutionContext ctx);

    /** The user prompt (required, may reference variables). */
    protected abstract String userPrompt(NodeExecutionContext ctx);

    /** Optional model override, or null for the provider default. */
    protected String model(NodeExecutionContext ctx) {
        return ctx.configString("model");
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String sys = interpolate(ctx, systemPrompt(ctx));
        String user = interpolate(ctx, userPrompt(ctx));
        String model = model(ctx);

        if (user == null || user.isBlank()) {
            return NodeResult.fail("This node requires a non-empty user prompt.");
        }

        AiCompleter.Completion completion = aiCompleter.complete(sys, user, model, jsonMode);

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("configured", completion.configured());
        if (!completion.configured()) {
            output.put("note", "AI is not configured. Set an AI provider key on the AI service to enable this node.");
            output.put("output", "");
            ctx.log().warn(this.type() + " ran but AI is not configured — no completion generated.");
            return NodeResult.success(output);
        }

        output.put("output", completion.output() == null ? "" : completion.output());
        if (completion.model() != null && !completion.model().isBlank()) {
            output.put("model", completion.model());
        }
        ctx.log().info(this.type() + " produced a completion.");
        return NodeResult.success(output);
    }

    private String interpolate(NodeExecutionContext ctx, String template) {
        if (template == null || template.isBlank()) {
            return "";
        }
        return ctx.interpolate(template);
    }
}