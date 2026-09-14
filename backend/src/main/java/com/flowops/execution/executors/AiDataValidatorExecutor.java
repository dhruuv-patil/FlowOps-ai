package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Data Validator: checks data against validation rules and reports findings. */
@Component
public class AiDataValidatorExecutor extends AiCompletionExecutor {

    public AiDataValidatorExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "ai_data_validator";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        String rules = String.join("; ", ctx.configStringList("rules"));
        return "Validate data against rules. Return ONLY JSON with keys: "
                + "valid (boolean), findings (array of {field, message, severity}). Rules: "
                + rules;
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
