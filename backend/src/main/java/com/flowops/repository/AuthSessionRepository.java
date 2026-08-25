package com.flowops.repository;

import com.flowops.domain.AuthSession;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    /** Used by refresh-token reuse detection: revoke everything this user has. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AuthSession s
               SET s.revokedAt = :now
             WHERE s.userId = :userId
               AND s.revokedAt IS NULL
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
