package com.flowops.notification;

import com.flowops.api.NotificationEnvelopes;
import com.flowops.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own notification inbox (M5, slice 4).
 *
 * <p>Both the user and the organization come from the principal, so there is no path
 * parameter or body field that could address another member's inbox. There is
 * deliberately no create endpoint: notifications are written only by the server, from
 * real run state.
 *
 * <p>The mark-read routes are {@code POST} with no body, so they carry no
 * {@code Content-Type} to constrain; state-changing verbs remain non-{@code GET}.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List my notifications in the current organization")
    public NotificationEnvelopes.Notifications list(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer limit) {
        return notificationService.list(AuthenticatedUser.require(), unreadOnly, limit);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one of my notifications read")
    public NotificationEnvelopes.Notifications markRead(@PathVariable UUID id) {
        return notificationService.markRead(AuthenticatedUser.require(), id);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all of my notifications read")
    public NotificationEnvelopes.Notifications markAllRead() {
        return notificationService.markAllRead(AuthenticatedUser.require());
    }
}
