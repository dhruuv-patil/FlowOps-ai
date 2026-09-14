package com.flowops.repository;

import com.flowops.domain.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Lookup is by SHA-256 hex only — the plaintext token is never stored.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds a valid (not used, not expired) reset token by hash.
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
             WHERE t.tokenHash = :tokenHash
               AND t.usedAt IS NULL
               AND t.expiresAt > :now
            """)
    Optional<PasswordResetToken> findValidByTokenHash(
            @Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Revokes all unused tokens for a user (e.g., after successful reset or new request).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
               SET t.usedAt = :now
             WHERE t.userId = :userId
               AND t.usedAt IS NULL
            """)
    int revokeAllUnusedForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}