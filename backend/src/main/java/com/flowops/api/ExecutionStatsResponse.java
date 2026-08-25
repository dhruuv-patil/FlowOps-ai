package com.flowops.api;

import java.time.Instant;
import java.util.List;

/**
 * Dashboard analytics over a time window: headline totals plus a bucketed time
 * series for the executions chart. Computed by the service from a lightweight
 * projection so a wide range never loads full run payloads.
 */
public record ExecutionStatsResponse(
        String range,
        Instant since,
        Totals totals,
        List<Point> series) {

    /**
     * @param successRate    succeeded / (succeeded + failed), 0 when neither has run
     * @param avgDurationMs  mean wall-clock of finished runs, null when none finished
     */
    public record Totals(
            long total,
            long succeeded,
            long failed,
            long running,
            long waiting,
            long queued,
            long canceled,
            double successRate,
            Long avgDurationMs) {
    }

    /** One time bucket: runs created within {@code [bucketStart, bucketStart+step)}. */
    public record Point(Instant bucketStart, long total, long succeeded, long failed) {
    }
}
