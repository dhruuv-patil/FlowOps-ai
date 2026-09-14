package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** Information Extractor: pulls structured fields from unstructured text. */
@Component
public class InformationExtractorExecutor extends AiCompletionExecutor {

    public InformationExtractorExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "information_extractor";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        String fields = String.join(", ", ctx.configStringList("fields"));
        return "Extract structured fields. Return ONLY a JSON object with the requested fields. "
                + "Fields to extract: " + fields;
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
