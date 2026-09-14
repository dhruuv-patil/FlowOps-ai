package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** Text Summarizer: produces a concise natural-language summary of input text. */
@Component
public class TextSummarizerExecutor extends AiCompletionExecutor {

    public TextSummarizerExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, false);
    }

    @Override
    public String type() {
        return "text_summarizer";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        String maxWords = ctx.configString("maxWords");
        if (maxWords == null || maxWords.isBlank()) {
            return "Summarize the following text concisely.";
        }
        return "Summarize the following text concisely. Max words: " + maxWords;
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
