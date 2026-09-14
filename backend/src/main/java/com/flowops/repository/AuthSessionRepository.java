package com.flowops.repository;

import com.flowops.domain.AuthSession;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    /**
     * Used by refresh-token reuse detection.
     *
     * Reuse of an already-consumed refresh token is treated as a possible
     * credential compromise, so every active session belonging to the user
     * must be revoked.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AuthSession s
               SET s.revokedAt = :now
             WHERE s.userId = :userId
               AND s.revokedAt IS NULL
            """)
    int revokeAllForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now);

    /**
     * Used after a password change.
     *
     * The current session remains alive while every other session belonging
     * to the user is revoked.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AuthSession s
               SET s.revokedAt = :now
             WHERE s.userId = :userId
               AND s.id <> :keepSessionId
               AND s.revokedAt IS NULL
            """)
    int revokeAllForUserExceptSession(
            @Param("userId") UUID userId,
            @Param("keepSessionId") UUID keepSessionId,
            @Param("now") Instant now);
}