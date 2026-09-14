package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Decision: returns a boolean decision with confidence and rationale. */
@Component
public class AiDecisionExecutor extends AiCompletionExecutor {

    public AiDecisionExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "ai_decision";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return "You are a decision engine. Return ONLY a JSON object with keys: "
                + "decision (boolean), confidence (0-1), rationale (string).";
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        String question = ctx.configString("question");
        String input = ctx.configString("input");
        if (question != null && input != null) {
            return question + "\n\n" + input;
        }
        return question != null ? question : input;
    }
}
