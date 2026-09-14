package com.flowops.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowops.api.NotificationEnvelopes;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.Notification;
import com.flowops.domain.NotificationLevel;
import com.flowops.domain.Role;
import com.flowops.repository.NotificationRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/**
 * Slice tests for {@link NotificationService}.
 *
 * <p>Every inbox operation is scoped to <em>both</em> the caller's user id and their
 * current organization, taken from the principal — so the invariants under test are that
 * one member can never read or mutate another's inbox, that a notification belonging to
 * someone else is indistinguishable from one that does not exist
 * ({@code NOTIFICATION_NOT_FOUND}), and that the server-side publish addresses the run's
 * creator when it has one and the org's admins when it does not.
 */
class NotificationServiceTest {

    private FlowOpsPrincipal member(UUID userId, UUID orgId) {
        return new FlowOpsPrincipal(
                userId, UUID.randomUUID(), orgId, Role.MEMBER, "member@example.com");
    }

    @Test
    void listingIsScopedToTheCallersUserAndOrganizationAndClampsTheLimit() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.search(any(), any(), anyBoolean(), any())).thenReturn(List.of());
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(repository.countByUserIdAndOrganizationIdAndReadAtIsNull(userId, orgId)).thenReturn(3);

        NotificationEnvelopes.Notifications inbox =
                new NotificationService(repository).list(member(userId, orgId), true, 10_000);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq(userId), eq(orgId), eq(true), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(100);
        // The unread count rides along so the badge needs no second call.
        assertThat(inbox.unreadCount()).isEqualTo(3);
    }

    @Test
    void anotherMembersNotificationIsIndistinguishableFromAbsent() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.findByIdAndUserIdAndOrganizationId(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new NotificationService(repository)
                        .markRead(member(UUID.randomUUID(), UUID.randomUUID()), id))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void markingAllReadTouchesOnlyTheCallersOwnRows() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.search(any(), any(), anyBoolean(), any())).thenReturn(List.of());
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        new NotificationService(repository).markAllRead(member(userId, orgId));

        verify(repository).markAllRead(eq(userId), eq(orgId), any());
    }

    @Test
    void markingOneReadIsIdempotentAndReturnsTheRefreshedInbox() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Notification notification = Notification.create(
                orgId, userId, NotificationLevel.ERROR, "A workflow run failed", "boom", "/executions/x");
        when(repository.findByIdAndUserIdAndOrganizationId(notification.getId(), userId, orgId))
                .thenReturn(Optional.of(notification));
        when(repository.search(any(), any(), anyBoolean(), any())).thenReturn(List.of(notification));

        NotificationService service = new NotificationService(repository);
        service.markRead(member(userId, orgId), notification.getId());
        java.time.Instant firstRead = notification.getReadAt();
        service.markRead(member(userId, orgId), notification.getId());

        assertThat(firstRead).isNotNull();
        assertThat(notification.getReadAt()).isEqualTo(firstRead);
    }

    @Test
    void aRunWithNoCreatorIsAddressedToTheOrganizationsAdmins() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UUID orgId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(repository.findAdminUserIds(orgId)).thenReturn(List.of(ownerId, adminId));

        new NotificationService(repository)
                .publish(orgId, null, NotificationLevel.ERROR, "A workflow run failed", "boom", "/executions/x");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(row -> assertThat(row.getOrganizationId()).isEqualTo(orgId));
        assertThat(saved.getAllValues().stream().map(Notification::getUserId))
                .containsExactlyInAnyOrder(ownerId, adminId);
    }

    @Test
    void aRunWithACreatorGoesOnlyToThatUser() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UUID orgId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        new NotificationService(repository)
                .publish(orgId, creatorId, NotificationLevel.WARN, "Needs approval", null, "/executions/x");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(creatorId);
        verify(repository, never()).findAdminUserIds(any());
    }
}
