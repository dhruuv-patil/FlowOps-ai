package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** JSON Generator: produces a JSON structure matching free-form instructions. */
@Component
public class JsonGeneratorExecutor extends AiCompletionExecutor {

    public JsonGeneratorExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "json_generator";
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
