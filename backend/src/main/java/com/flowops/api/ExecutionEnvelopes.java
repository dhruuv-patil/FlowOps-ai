package com.flowops.api;

import java.util.List;

/** Object envelopes for the execution collection endpoints (never bare arrays). */
public final class ExecutionEnvelopes {

    private ExecutionEnvelopes() {
    }

    public record Executions(List<ExecutionSummaryResponse> executions) {
    }
}
