package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * An in-app message addressed to exactly one user within one organization.
 *
 * <p>Every notification is derived from real state — a run that failed, a run parked on
 * a human approval. Nothing here is seeded or synthetic.
 *
 * <p>{@code link} is always a relative in-app path (e.g. {@code /executions/{id}}) so a
 * notification can never point off-origin. {@code readAt} is the only mutable column.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 8, updatable = false)
    private NotificationLevel level;

    @Column(name = "title", nullable = false, length = 200, updatable = false)
    private String title;

    @Column(name = "body", length = 500, updatable = false)
    private String body;

    @Column(name = "link", length = 300, updatable = false)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public static Notification create(
            UUID organizationId,
            UUID userId,
            NotificationLevel level,
            String title,
            String body,
            String link) {
        Notification notification = new Notification();
        notification.id = UUID.randomUUID();
        notification.organizationId = organizationId;
        notification.userId = userId;
        notification.level = level;
        notification.title = title;
        notification.body = body;
        notification.link = link;
        notification.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return notification;
    }

    /** Marks this notification read. Idempotent: the first read timestamp is kept. */
    public void markRead() {
        if (readAt == null) {
            this.readAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getLink() {
        return link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
