package com.flowops.repository;

import com.flowops.domain.AuditAction;
import com.flowops.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The audit trail. Reads are always org-scoped — there is no bare {@code findById} for
 * callers (contract §2 rule 6) — and there is deliberately no update or delete path:
 * the trail is append-only.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Org-scoped trail, newest first, with an optional {@code action} filter (null
     * disables the clause). The caller passes a {@link Pageable} to cap the row count.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.organizationId = :organizationId
              AND (:action IS NULL OR a.action = :action)
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> search(
            @Param("organizationId") UUID organizationId,
            @Param("action") AuditAction action,
            Pageable pageable);
}
