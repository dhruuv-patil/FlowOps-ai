package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Content Generator: produces natural-language content from a creative brief. */
@Component
public class AiContentGeneratorExecutor extends AiCompletionExecutor {

    public AiContentGeneratorExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, false);
    }

    @Override
    public String type() {
        return "ai_content_generator";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return ctx.configString("brief");
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
