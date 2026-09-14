package com.flowops.api;

import com.flowops.domain.Notification;
import com.flowops.domain.NotificationLevel;
import java.time.Instant;
import java.util.UUID;

/**
 * One in-app notification as returned to its addressee. {@code link} is always a relative
 * in-app path, so rendering it as an anchor can never navigate off-origin.
 */
public record NotificationResponse(
        UUID id,
        NotificationLevel level,
        String title,
        String body,
        String link,
        Instant readAt,
        Instant createdAt) {

    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getLevel(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLink(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
