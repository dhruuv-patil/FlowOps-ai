package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Researcher: answers a question using only provided context, with inline citations. */
@Component
public class AiResearcherExecutor extends AiCompletionExecutor {

    public AiResearcherExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, false);
    }

    @Override
    public String type() {
        return "ai_researcher";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return "Answer the question using only the provided context. "
                + "Cite sources inline. If the answer isn't in the context, say so.";
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        String question = ctx.configString("question");
        String context = ctx.configString("input");
        return "Question: " + question + "\n\nContext:\n" + context;
    }
}
