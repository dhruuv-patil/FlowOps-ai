package com.flowops.api;

import com.flowops.domain.AuditAction;
import com.flowops.domain.AuditLog;
import java.time.Instant;
import java.util.UUID;

/**
 * One audit-trail entry as returned to an admin. Every field is non-secret by
 * construction — the trail never stores a credential, so there is nothing to mask here.
 * {@code actorUserId}/{@code actorEmail} are null for anonymous events such as a failed
 * inbound-webhook authentication.
 */
public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorEmail,
        AuditAction action,
        String targetType,
        String targetId,
        String summary,
        String ip,
        Instant createdAt) {

    public static AuditLogResponse of(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSummary(),
                log.getIp(),
                log.getCreatedAt());
    }
}
