package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** Sentiment Analyzer: detects sentiment polarity and numeric score. */
@Component
public class SentimentAnalyzerExecutor extends AiCompletionExecutor {

    public SentimentAnalyzerExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "sentiment_analyzer";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return "Analyze sentiment. Return ONLY JSON with keys: "
                + "sentiment (positive|negative|neutral), score (-1 to 1).";
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
