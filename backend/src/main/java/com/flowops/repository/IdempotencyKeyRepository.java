package com.flowops.repository;

import com.flowops.domain.IdempotencyKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Idempotency key store for safe replay of webhook calls.
 * Keys are scoped by workflow to prevent cross-workflow collisions.
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /** Find by composite key (e.g., "webhook:{workflowId}:{callerKey}"). */
    Optional<IdempotencyKey> findByKey(@Param("key") String key);

    /** Clean up expired keys periodically. */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpired(@Param("now") java.time.Instant now);
}