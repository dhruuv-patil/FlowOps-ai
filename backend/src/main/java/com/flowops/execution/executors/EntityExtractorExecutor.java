package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** Entity Extractor: pulls named entities from text with position offsets. */
@Component
public class EntityExtractorExecutor extends AiCompletionExecutor {

    public EntityExtractorExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "entity_extractor";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        var types = ctx.configStringList("entityTypes");
        String typesText = types.isEmpty() ? "all" : String.join(", ", types);
        return "Extract entities. Return ONLY JSON array of objects with keys: "
                + "text, type, start, end. Types to extract: " + typesText;
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
