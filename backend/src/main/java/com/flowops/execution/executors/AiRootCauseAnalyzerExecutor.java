package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Root Cause Analyzer: identifies likely root causes from failure evidence. */
@Component
public class AiRootCauseAnalyzerExecutor extends AiCompletionExecutor {

    public AiRootCauseAnalyzerExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "ai_root_cause_analyzer";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return "Analyze failure evidence and return likely root causes. "
                + "Return ONLY JSON array of objects with keys: "
                + "cause, category (code-change|provider-change|data-quality|infrastructure|configuration|unknown), "
                + "confidence (0-1), uncertainty.";
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
