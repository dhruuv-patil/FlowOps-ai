package com.flowops.api;

import java.util.List;

/** Object envelopes for the org-wide log endpoint (never a bare array). */
public final class LogEnvelopes {

    private LogEnvelopes() {
    }

    public record Logs(List<LogEntryResponse> logs) {
    }
}
