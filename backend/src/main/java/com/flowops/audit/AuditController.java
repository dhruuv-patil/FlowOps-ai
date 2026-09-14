package com.flowops.audit;

import com.flowops.api.AuditEnvelopes;
import com.flowops.domain.AuditAction;
import com.flowops.domain.Role;
import com.flowops.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only access to the organization's audit trail (M5, slice 4).
 *
 * <p>The tenant comes from the principal; no endpoint accepts an organization id. Gated
 * to {@link Role#ADMIN} and above: the trail records who changed roles, who connected a
 * credential and whose password changed, which is administrative rather than member data.
 * There is deliberately no write, update, or delete endpoint — the trail is append-only
 * and is written only by the server, at the actions it records.
 */
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List audit-trail entries for the current organization")
    public AuditEnvelopes.AuditLogs list(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Integer limit) {
        return auditService.list(AuthenticatedUser.requireRole(Role.ADMIN), action, limit);
    }
}
