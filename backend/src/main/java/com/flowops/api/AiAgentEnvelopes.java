package com.flowops.api;

import java.util.List;

/** Object envelopes for the AI agent collection endpoints (never bare arrays). */
public final class AiAgentEnvelopes {

    private AiAgentEnvelopes() {
    }

    public record Agents(List<AiAgentSummaryResponse> agents) {
    }
}
