package com.flowops.repository;

import com.flowops.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t
               SET t.revokedAt = :now
             WHERE t.sessionId = :sessionId
               AND t.revokedAt IS NULL
            """)
    int revokeAllForSession(
            @Param("sessionId") UUID sessionId,
            @Param("now") Instant now);

    /**
     * Reuse detection is destructive by design:
     * every refresh token for every session belonging to the user is revoked.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t
               SET t.revokedAt = :now
             WHERE t.revokedAt IS NULL
               AND t.sessionId IN (
                   SELECT s.id
                   FROM AuthSession s
                   WHERE s.userId = :userId
               )
            """)
    int revokeAllForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now);

    /**
     * Used when changing a password:
     * revoke every refresh token except the caller's current session.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t
               SET t.revokedAt = :now
             WHERE t.revokedAt IS NULL
               AND t.sessionId <> :keepSessionId
               AND t.sessionId IN (
                   SELECT s.id
                   FROM AuthSession s
                   WHERE s.userId = :userId
               )
            """)
    int revokeAllForUserExceptSession(
            @Param("userId") UUID userId,
            @Param("keepSessionId") UUID keepSessionId,
            @Param("now") Instant now);
}