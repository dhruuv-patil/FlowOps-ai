package com.flowops.audit;

import com.flowops.api.AuditEnvelopes;
import com.flowops.api.AuditLogResponse;
import com.flowops.common.ratelimit.RateLimiter;
import com.flowops.domain.AuditAction;
import com.flowops.domain.AuditLog;
import com.flowops.repository.AuditLogRepository;
import com.flowops.security.FlowOpsPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes and reads the organization's audit trail.
 *
 * <p><strong>Writes use {@link Propagation#REQUIRES_NEW}.</strong> Audited actions are
 * reached from three different transactional contexts — normal read-write services, the
 * {@code readOnly = true} webhook verification path, and non-transactional pre-auth code
 * — and the trail must persist even if the surrounding business transaction later rolls
 * back (a failed webhook authentication is exactly the event worth keeping). A suspended
 * outer transaction plus its own commit gives that in every case.
 *
 * <p>A write must never break the action it records: {@link #record} swallows persistence
 * failures rather than surfacing them, so an audit outage cannot deny service.
 *
 * <p>Contract §9: nothing secret is passed in. Call sites compose {@code summary} from
 * role names, provider types and masked last-4 hints only — never a token, webhook URL,
 * or password. The client IP is read from the current request when there is one (engine
 * threads have none) and is <em>never</em> taken from a client-supplied forwarding header.
 *
 * <p>Reading is org-scoped and, at the controller, gated to admins: the trail is the
 * tenant's own tamper-evidence surface, not general member-visible data.
 */
@Service
public class AuditService {

    /** Same clamped-limit contract as the executions list; the backend has no pagination. */
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    /**
     * Records an action taken by an authenticated principal in their own organization.
     *
     * <p>Annotated in its own right rather than delegating to the overload below: a
     * self-invocation would bypass the proxy and lose {@code REQUIRES_NEW}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            FlowOpsPrincipal principal, AuditAction action, String targetId, String summary) {
        save(
                principal.organizationId(),
                principal.userId(),
                principal.email(),
                action,
                targetId,
                summary);
    }

    /**
     * Records an action in an explicit organization. Used when the affected tenant is not
     * the principal's current one (accepting an invitation joins a different org) and for
     * anonymous events, where {@code actorUserId} and {@code actorEmail} are null.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            AuditAction action,
            String targetId,
            String summary) {
        save(organizationId, actorUserId, actorEmail, action, targetId, summary);
    }

    private void save(
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            AuditAction action,
            String targetId,
            String summary) {
        try {
            auditLogs.save(AuditLog.create(
                    organizationId, actorUserId, actorEmail, action, targetId, summary, currentIp()));
        } catch (RuntimeException auditFailure) {
            // An audit write must never deny the action it records. Nothing is logged
            // here either: the surrounding call may be handling a secret.
        }
    }

    /** Org-scoped trail, newest first, optionally filtered by action. */
    @Transactional(readOnly = true)
    public AuditEnvelopes.AuditLogs list(
            FlowOpsPrincipal principal, AuditAction action, Integer limit) {
        List<AuditLogResponse> rows = auditLogs
                .search(principal.organizationId(), action, PageRequest.of(0, clampLimit(limit)))
                .stream()
                .map(AuditLogResponse::of)
                .toList();
        return new AuditEnvelopes.AuditLogs(rows);
    }

    private static int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * The address of the request being served, or null when there is none (the execution
     * engine runs on its own threads). Read from the servlet request itself, so a spoofed
     * {@code X-Forwarded-For} can never land in the trail.
     */
    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return RateLimiter.clientIp(request);
        }
        return null;
    }
}
