package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** Text Classifier: classifies input text into provided categories. */
@Component
public class TextClassifierExecutor extends AiCompletionExecutor {

    public TextClassifierExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "text_classifier";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        String categories = String.join(", ", ctx.configStringList("categories"));
        return "You are a classifier. Return ONLY a JSON object with keys: "
                + "category (one of the provided categories), confidence (0-1). Categories: "
                + categories;
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
