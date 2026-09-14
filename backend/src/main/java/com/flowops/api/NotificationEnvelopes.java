package com.flowops.api;

import java.util.List;

/** Object envelopes for the notification endpoints (never bare arrays). */
public final class NotificationEnvelopes {

    private NotificationEnvelopes() {
    }

    /** The inbox listing, carrying the unread count so the badge needs no second call. */
    public record Notifications(List<NotificationResponse> notifications, int unreadCount) {
    }
}
