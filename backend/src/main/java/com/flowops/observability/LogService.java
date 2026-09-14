package com.flowops.observability;

import com.flowops.api.LogEntryResponse;
import com.flowops.api.LogEnvelopes;
import com.flowops.domain.LogLevel;
import com.flowops.repository.ExecutionLogRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The org-wide log viewer.
 *
 * <p>This reuses the execution logs the engine already writes — it does not introduce a
 * second logging system. {@code execution_logs} carries no {@code organization_id} (it is
 * append-only run detail), so tenancy is resolved by joining through
 * {@code workflow_executions}; see
 * {@link ExecutionLogRepository#searchForOrganization}. The organization always comes from
 * the principal, so a run in another tenant simply has no rows to return.
 *
 * <p>Like every other list in this API there is no pagination: an optional workflow and
 * level filter plus a clamped limit, matching {@code ExecutionService}.
 */
@Service
public class LogService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final ExecutionLogRepository logs;

    public LogService(ExecutionLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public LogEnvelopes.Logs list(
            FlowOpsPrincipal principal, UUID workflowId, LogLevel level, Integer limit) {
        List<LogEntryResponse> rows = logs
                .searchForOrganization(
                        principal.organizationId(),
                        workflowId,
                        level,
                        PageRequest.of(0, clampLimit(limit)))
                .stream()
                .map(LogEntryResponse::of)
                .toList();
        return new LogEnvelopes.Logs(rows);
    }

    private static int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
