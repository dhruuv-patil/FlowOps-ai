package com.flowops.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Chart data for the workflow reliability widget (reliability pivot, M4): aggregate
 * success/latency KPIs plus a bucketed time series derived from node telemetry.
 */
public record ReliabilityMetricsResponse(
        UUID workflowId,
        double successRate,
        int executions,
        List<Bucket> series) {

    /** One time bucket (e.g. a day): executions, the failure count, avg node duration. */
    public record Bucket(
            Instant from,
            int executions,
            int failed,
            double avgDurationMs) {
    }
}
