package com.flowops.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowops.domain.AuditAction;
import com.flowops.domain.AuditLog;
import com.flowops.domain.Role;
import com.flowops.repository.AuditLogRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/**
 * Slice tests for {@link AuditService}. The repository is mocked; what is under test is
 * the trail's security posture rather than persistence.
 *
 * <p>Invariants: a write takes its organization and actor from the principal (never from
 * a caller-supplied field); a read is scoped to the principal's organization, so one
 * tenant's trail can never be widened into another's; the limit is clamped; and a
 * persistence failure is swallowed, because an audit outage must not deny the action it
 * was recording.
 */
class AuditServiceTest {

    private FlowOpsPrincipal admin(UUID orgId) {
        return new FlowOpsPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), orgId, Role.ADMIN, "admin@example.com");
    }

    @Test
    void aWriteTakesTheOrganizationAndActorFromThePrincipal() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        UUID orgId = UUID.randomUUID();
        FlowOpsPrincipal principal = admin(orgId);

        new AuditService(repository)
                .record(principal, AuditAction.MEMBER_REMOVED, "target-id", "Removed a MEMBER.");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOrganizationId()).isEqualTo(orgId);
        assertThat(saved.getValue().getActorUserId()).isEqualTo(principal.userId());
        assertThat(saved.getValue().getActorEmail()).isEqualTo(principal.email());
        // The target type is derived from the action, so a call site cannot mislabel it.
        assertThat(saved.getValue().getTargetType()).isEqualTo(AuditAction.MEMBER_REMOVED.targetType());
        // No request is bound on a plain unit-test thread, so no IP is invented.
        assertThat(saved.getValue().getIp()).isNull();
    }

    @Test
    void anAnonymousWriteRecordsAnExplicitOrganizationAndNoActor() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        UUID orgId = UUID.randomUUID();

        new AuditService(repository)
                .record(
                        orgId,
                        null,
                        null,
                        AuditAction.WEBHOOK_AUTH_FAILED,
                        UUID.randomUUID().toString(),
                        "Rejected an inbound webhook call: the token did not match.");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOrganizationId()).isEqualTo(orgId);
        assertThat(saved.getValue().getActorUserId()).isNull();
        assertThat(saved.getValue().getActorEmail()).isNull();
    }

    @Test
    void aReadIsScopedToThePrincipalsOrganizationAndClampsTheLimit() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.search(any(), any(), any())).thenReturn(List.of());
        UUID orgId = UUID.randomUUID();

        new AuditService(repository).list(admin(orgId), null, 10_000);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq(orgId), isNull(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(200);
        assertThat(page.getValue().getPageNumber()).isZero();
    }

    @Test
    void aFailedAuditWriteNeverDeniesTheActionItRecords() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(any(AuditLog.class))).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> new AuditService(repository)
                        .record(
                                admin(UUID.randomUUID()),
                                AuditAction.ORGANIZATION_RENAMED,
                                "org",
                                "Renamed the organization."))
                .doesNotThrowAnyException();
    }
}
