package com.flowops.execution.executors;

import com.flowops.ai.AiCompleter;
import com.flowops.execution.NodeExecutionContext;
import org.springframework.stereotype.Component;

/** AI Anomaly Investigator: deep-dives into anomalies and recommends remediation. */
@Component
public class AiAnomalyInvestigatorExecutor extends AiCompletionExecutor {

    public AiAnomalyInvestigatorExecutor(AiCompleter aiCompleter) {
        super(aiCompleter, true);
    }

    @Override
    public String type() {
        return "ai_anomaly_investigator";
    }

    @Override
    protected String systemPrompt(NodeExecutionContext ctx) {
        return "Investigate anomaly from evidence. Return ONLY JSON with keys: "
                + "summary, likelyCauses (array of {cause, category, confidence, uncertainty}), "
                + "evidence (array of {type, description, source, inference}), "
                + "impact, recommendedActions (array of {action, rationale, risk, effort}), "
                + "confidence (0-1).";
    }

    @Override
    protected String userPrompt(NodeExecutionContext ctx) {
        return ctx.configString("input");
    }
}
