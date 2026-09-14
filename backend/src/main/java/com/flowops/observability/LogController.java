package com.flowops.observability;

import com.flowops.api.LogEnvelopes;
import com.flowops.domain.LogLevel;
import com.flowops.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The org-wide log viewer (M5, slice 4). Readable by any member: these are the run logs
 * they can already reach one execution at a time, surfaced in one place.
 *
 * <p>The tenant comes from the principal — a {@code workflowId} filter can only narrow
 * within the caller's own organization, never widen outside it.
 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    @Operation(summary = "List recent execution log lines across the current organization")
    public LogEnvelopes.Logs list(
            @RequestParam(required = false) UUID workflowId,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) Integer limit) {
        return logService.list(AuthenticatedUser.require(), workflowId, level, limit);
    }
}
