package com.flowops.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowops.domain.LogLevel;
import com.flowops.domain.Role;
import com.flowops.repository.ExecutionLogRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/**
 * Slice tests for {@link LogService}: the org-wide viewer over the logs the engine already
 * writes.
 *
 * <p>The invariant under test is that the organization always comes from the principal.
 * The optional {@code workflowId} filter is passed straight through as a <em>narrowing</em>
 * clause alongside that org id, so naming another tenant's workflow cannot widen the query
 * — the join in {@code searchForOrganization} still constrains rows to the caller's org and
 * simply returns nothing.
 */
class LogServiceTest {

    private FlowOpsPrincipal viewer(UUID orgId) {
        return new FlowOpsPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), orgId, Role.VIEWER, "viewer@example.com");
    }

    @Test
    void theOrganizationComesFromThePrincipalAndTheLimitIsClamped() {
        ExecutionLogRepository repository = mock(ExecutionLogRepository.class);
        when(repository.searchForOrganization(any(), any(), any(), any())).thenReturn(List.of());
        UUID orgId = UUID.randomUUID();

        new LogService(repository).list(viewer(orgId), null, null, 10_000);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).searchForOrganization(eq(orgId), isNull(), isNull(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(500);
        assertThat(page.getValue().getPageNumber()).isZero();
    }

    @Test
    void anAbsentLimitFallsBackToTheDefault() {
        ExecutionLogRepository repository = mock(ExecutionLogRepository.class);
        when(repository.searchForOrganization(any(), any(), any(), any())).thenReturn(List.of());

        new LogService(repository).list(viewer(UUID.randomUUID()), null, null, null);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).searchForOrganization(any(), isNull(), isNull(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void aWorkflowFilterNarrowsWithinTheCallersOrganizationRatherThanReplacingIt() {
        ExecutionLogRepository repository = mock(ExecutionLogRepository.class);
        when(repository.searchForOrganization(any(), any(), any(), any())).thenReturn(List.of());
        UUID orgId = UUID.randomUUID();
        UUID foreignWorkflowId = UUID.randomUUID();

        new LogService(repository).list(viewer(orgId), foreignWorkflowId, LogLevel.ERROR, 25);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        // The caller's org id is still the first argument: the filter is additive.
        verify(repository)
                .searchForOrganization(eq(orgId), eq(foreignWorkflowId), eq(LogLevel.ERROR), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(25);
    }
}
