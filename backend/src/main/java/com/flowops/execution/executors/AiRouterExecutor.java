package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Router: routes input to the best-matching path via a JSON response. */
@Component
public class AiRouterExecutor extends AiCompletionExecutor {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a router. Return ONLY a JSON object with a single key 'route' "
            + "whose value is one of the allowed routes.";

    public AiRouterExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "ai_router";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        String instructions = ctx.configString("instructions");
        if (instructions == null || instructions.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return instructions;
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
