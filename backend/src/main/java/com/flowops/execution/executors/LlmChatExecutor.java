package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** LLM Chat: a single-turn chat completion with system instructions and user input. */
@Component
public class LlmChatExecutor extends AiCompletionExecutor {

    public LlmChatExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, false);
    }

    @Override
    public String type() {
        return "llm_chat";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return ctx.configString("instructions");
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}