package com.flowops.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowops.audit.AuditService;
import com.flowops.domain.AuditAction;
import com.flowops.domain.WorkflowExecution;
import com.flowops.repository.WorkflowExecutionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ExecutionRecoveryService}. The store and repositories are
 * mocked so the test stays fast and offline; the only thing under test is the
 * sweep's loop logic, not the engine or queue.
 *
 * <p>Invariants covered:
 * <ul>
 *   <li>an empty recoverable set is a no-op (no enqueues, no audit rows);</li>
 *   <li>every recoverable id is re-enqueued exactly once and the audit trail
 *       receives one {@code EXECUTION_RECOVERED} row per id, scoped to the
 *       execution's own organization;</li>
 *   <li>a per-execution failure does not stop the loop from continuing with the
 *       next run, and the failed id is left wedged (the sweep does not
 *       pretend it was recovered).</li>
 * </ul>
 */
class ExecutionRecoveryServiceTest {

    @Test
    void emptyRecoverableSetIsANoOp() {
        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        when(executions.findRecoverableExecutionIds())
                .thenReturn(List.of());

        ExecutionStore store = mock(ExecutionStore.class);
        QueueService queue = mock(QueueService.class);
        AuditService audit = mock(AuditService.class);

        int requeued =
                new ExecutionRecoveryService(executions, store, queue, audit)
                        .recover();

        assertThat(requeued).isZero();

        verify(queue, never()).enqueue(any());

        verify(audit, never()).record(
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString());
    }

    @Test
    void everyRecoverableExecutionIsReEnqueuedAndAuditedInItsOwnOrg() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        when(executions.findRecoverableExecutionIds())
                .thenReturn(List.of(a, b));

        ExecutionStore store = mock(ExecutionStore.class);

        /*
         * Create and fully stub the execution mocks BEFORE using them inside
         * another Mockito when(...).thenReturn(...).
         *
         * This avoids nested Mockito stubbing, which causes
         * UnfinishedStubbingException.
         */
        WorkflowExecution executionA = stubExecution(a, orgA);
        WorkflowExecution executionB = stubExecution(b, orgB);

        when(store.getExecution(a)).thenReturn(executionA);
        when(store.getExecution(b)).thenReturn(executionB);

        when(store.resetStuckNodesForRecovery(a)).thenReturn(2);
        when(store.resetStuckNodesForRecovery(b)).thenReturn(0);

        QueueService queue = mock(QueueService.class);
        AuditService audit = mock(AuditService.class);

        int requeued =
                new ExecutionRecoveryService(executions, store, queue, audit)
                        .recover();

        assertThat(requeued).isEqualTo(2);

        verify(queue, times(1)).enqueue(a);
        verify(queue, times(1)).enqueue(b);

        verify(audit).record(
                eq(orgA),
                eq(null),
                eq(null),
                eq(AuditAction.EXECUTION_RECOVERED),
                eq(a.toString()),
                anyString());

        verify(audit).record(
                eq(orgB),
                eq(null),
                eq(null),
                eq(AuditAction.EXECUTION_RECOVERED),
                eq(b.toString()),
                anyString());
    }

    @Test
    void aFailureOnOneRunDoesNotBlockTheNext() {
        UUID good = UUID.randomUUID();
        UUID bad = UUID.randomUUID();
        UUID orgGood = UUID.randomUUID();
        UUID orgBad = UUID.randomUUID();

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        when(executions.findRecoverableExecutionIds())
                .thenReturn(List.of(bad, good));

        ExecutionStore store = mock(ExecutionStore.class);

        /*
         * Again, fully create/stub the execution mocks first so Mockito does
         * not encounter nested stubbing inside thenReturn(...).
         */
        WorkflowExecution badExecution = stubExecution(bad, orgBad);
        WorkflowExecution goodExecution = stubExecution(good, orgGood);

        when(store.getExecution(bad)).thenReturn(badExecution);
        when(store.getExecution(good)).thenReturn(goodExecution);

        /*
         * Reset fails for bad; succeeds for good.
         * The recovery sweep should catch the bad execution's exception and
         * continue processing the good execution.
         */
        doThrow(new RuntimeException("db hiccup"))
                .when(store)
                .resetStuckNodesForRecovery(bad);

        when(store.resetStuckNodesForRecovery(good))
                .thenReturn(1);

        QueueService queue = mock(QueueService.class);
        AuditService audit = mock(AuditService.class);

        int requeued =
                new ExecutionRecoveryService(executions, store, queue, audit)
                        .recover();

        /*
         * The failed execution must not be enqueued.
         */
        verify(queue, never()).enqueue(bad);

        /*
         * The successful execution must be enqueued exactly once.
         */
        verify(queue, times(1)).enqueue(good);

        assertThat(requeued).isEqualTo(1);

        /*
         * No recovery audit should be written for the failed execution.
         */
        verify(audit, never()).record(
                eq(orgBad),
                any(),
                any(),
                eq(AuditAction.EXECUTION_RECOVERED),
                eq(bad.toString()),
                anyString());

        /*
         * The successful execution gets exactly one recovery audit row,
         * scoped to its own organization.
         */
        verify(audit).record(
                eq(orgGood),
                eq(null),
                eq(null),
                eq(AuditAction.EXECUTION_RECOVERED),
                eq(good.toString()),
                anyString());
    }

    /**
     * Creates a fully stubbed {@link WorkflowExecution} mock.
     *
     * <p>The recovery service only reads {@code getId()} and
     * {@code getOrganizationId()}, so those are the only methods that need
     * to be stubbed.
     */
    private static WorkflowExecution stubExecution(UUID id, UUID orgId) {
        WorkflowExecution exec = mock(WorkflowExecution.class);

        when(exec.getId()).thenReturn(id);
        when(exec.getOrganizationId()).thenReturn(orgId);

        return exec;
    }
}