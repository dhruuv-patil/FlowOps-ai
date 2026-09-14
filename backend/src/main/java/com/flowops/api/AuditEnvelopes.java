package com.flowops.api;

import java.util.List;

/** Object envelopes for the audit-trail collection endpoint (never a bare array). */
public final class AuditEnvelopes {

    private AuditEnvelopes() {
    }

    public record AuditLogs(List<AuditLogResponse> auditLogs) {
    }
}
