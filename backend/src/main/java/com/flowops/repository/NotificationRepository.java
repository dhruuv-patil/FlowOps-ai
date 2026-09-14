package com.flowops.repository;

import com.flowops.domain.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * A user's personal notification inbox. Every read is keyed by {@code userId} as well as
 * the organization, so one member can never see another's inbox and no bare
 * {@code findById} is exposed to callers (contract §2 rule 6).
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** One notification, only if it belongs to this user in this organization. */
    Optional<Notification> findByIdAndUserIdAndOrganizationId(
            UUID id, UUID userId, UUID organizationId);

    /**
     * The inbox: this user's notifications in this org, newest first, optionally
     * restricted to unread ones. The caller passes a {@link Pageable} to cap the count.
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.userId = :userId
              AND n.organizationId = :organizationId
              AND (:unreadOnly = FALSE OR n.readAt IS NULL)
            ORDER BY n.createdAt DESC
            """)
    List<Notification> search(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    /** Backs the unread badge; served by the partial index on (user_id) WHERE read_at IS NULL. */
    int countByUserIdAndOrganizationIdAndReadAtIsNull(UUID userId, UUID organizationId);

    /**
     * Marks this user's whole inbox read in one statement. Scoped by user AND org, so it
     * can never touch another member's rows.
     */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :now
            WHERE n.userId = :userId
              AND n.organizationId = :organizationId
              AND n.readAt IS NULL
            """)
    int markAllRead(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId,
            @Param("now") java.time.Instant now);

    /** Addressees for a run notification when the run has no creator (webhook runs). */
    @Query("""
            SELECT m.userId FROM OrganizationMember m
            WHERE m.organizationId = :organizationId
              AND m.role IN (com.flowops.domain.Role.OWNER, com.flowops.domain.Role.ADMIN)
            """)
    List<UUID> findAdminUserIds(@Param("organizationId") UUID organizationId);
}
