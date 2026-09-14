package com.flowops.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.ExecutionNode;
import com.flowops.domain.ExecutionStatus;
import com.flowops.domain.NodeRunStatus;
import com.flowops.domain.WorkflowExecution;
import com.flowops.repository.ExecutionLogRepository;
import com.flowops.repository.ExecutionNodeRepository;
import com.flowops.repository.IdempotencyKeyRepository;
import com.flowops.repository.WorkflowExecutionRepository;
import com.flowops.repository.WorkflowRepository;
import com.flowops.repository.WorkflowVersionRepository;
import com.flowops.security.FlowOpsPrincipal;
import com.flowops.workflow.nodes.NodeRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Slice test for {@link ExecutionService#cancel}. The repositories and the queue
 * are mocked; a real {@link NodeRegistry} and {@link ObjectMapper} are not needed
 * because cancel does not parse a graph.
 *
 * <p>Invariants covered:
 * <ul>
 *   <li>A {@code RUNNING} run is flipped to {@code CANCELED}, every
 *       {@code RUNNING} or {@code PENDING} node row is marked
 *       {@code FAILED} with the {@code "Canceled by user."} message, and
 *       already-resolved nodes are left untouched.</li>
 *   <li>A {@code WAITING} run cannot be canceled (the human still has to
 *       decide) — the service raises {@link ErrorCode#EXECUTION_NOT_CANCELABLE}.</li>
 *   <li>Already-terminal runs (SUCCEEDED, FAILED, CANCELED) are a no-op: no
 *       node rows are mutated and no exception is thrown.</li>
 *   <li>Cross-tenant access always surfaces as {@link ErrorCode#EXECUTION_NOT_FOUND}.</li>
 * </ul>
 */
class ExecutionServiceCancelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final com.flowops.config.ExecutionProperties EXECUTION_PROPERTIES =
            new com.flowops.config.ExecutionProperties(
                    4,
                    3,
                    java.time.Duration.ofSeconds(2),
                    java.time.Duration.ofSeconds(30),
                    300,
                    java.time.Duration.ofSeconds(5),
                    java.time.Duration.ofSeconds(30),
                    1_048_576,
                    java.time.Duration.ofMinutes(30));

    private ExecutionService service(
            WorkflowRepository workflows,
            WorkflowVersionRepository versions,
            WorkflowExecutionRepository executions,
            ExecutionNodeRepository nodes,
            QueueService queue) {

        return new ExecutionService(
                workflows,
                versions,
                executions,
                nodes,
                mock(ExecutionLogRepository.class),
                queue,
                mock(ExecutionEvents.class),
                EXECUTION_PROPERTIES,
                new NodeRegistry(),
                MAPPER,
                mock(IdempotencyKeyRepository.class)
        );
    }

    private FlowOpsPrincipal principal(UUID orgId, UUID userId) {
        return new FlowOpsPrincipal(
                userId,
                UUID.randomUUID(),
                orgId,
                com.flowops.domain.Role.OWNER,
                "u@example.com");
    }

    private WorkflowExecution runningExecution(UUID orgId) {
        WorkflowExecution execution = mock(WorkflowExecution.class);

        when(execution.getId()).thenReturn(UUID.randomUUID());
        when(execution.getOrganizationId()).thenReturn(orgId);
        when(execution.getWorkflowId()).thenReturn(UUID.randomUUID());
        when(execution.getStatus()).thenReturn(ExecutionStatus.RUNNING);
        when(execution.getCreatedAt()).thenReturn(Instant.now());

        return execution;
    }

    private ExecutionNode runningNode(String id) {
        ExecutionNode node = mock(ExecutionNode.class);

        when(node.getNodeId()).thenReturn(id);
        when(node.getStatus()).thenReturn(NodeRunStatus.RUNNING);
        when(node.getLabel()).thenReturn(id);
        when(node.getStartedAt()).thenReturn(Instant.now());

        return node;
    }

    private ExecutionNode succeededNode(String id) {
        ExecutionNode node = mock(ExecutionNode.class);

        when(node.getNodeId()).thenReturn(id);
        when(node.getStatus()).thenReturn(NodeRunStatus.SUCCEEDED);
        when(node.getLabel()).thenReturn(id);

        return node;
    }

    private ExecutionNode pendingNode(String id) {
        ExecutionNode node = mock(ExecutionNode.class);

        when(node.getNodeId()).thenReturn(id);
        when(node.getStatus()).thenReturn(NodeRunStatus.PENDING);
        when(node.getLabel()).thenReturn(id);

        return node;
    }

    @Test
    void cancelFlipsRunningExecutionAndResetsInFlightNodes() {
        UUID orgId = UUID.randomUUID();

        WorkflowExecution execution = runningExecution(orgId);

        WorkflowRepository workflows = mock(WorkflowRepository.class);

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        when(executions.findByIdAndOrganizationId(execution.getId(), orgId))
                .thenReturn(Optional.of(execution));

        ExecutionNodeRepository nodes =
                mock(ExecutionNodeRepository.class);

        ExecutionNode running = runningNode("n1");
        ExecutionNode succeeded = succeededNode("n0");
        ExecutionNode pending = pendingNode("n2");

        when(nodes.findByExecutionIdOrderByCreatedAtAsc(execution.getId()))
                .thenReturn(List.of(succeeded, running, pending));

        WorkflowExecutionRepository executionsForService = executions;

        ExecutionService svc = service(
                workflows,
                mock(WorkflowVersionRepository.class),
                executionsForService,
                nodes,
                mock(QueueService.class));

        svc.cancel(
                principal(orgId, UUID.randomUUID()),
                execution.getId());

        // The execution is marked CANCELED. The succeeded node is left alone;
        // the running and pending nodes are each marked FAILED exactly once.
        verify(execution).markCanceled();
        verify(executionsForService).save(execution);

        verify(running).fail("Canceled by user.");
        verify(running, never()).succeed(any(), any());

        verify(pending).fail("Canceled by user.");

        verify(succeeded, never()).fail(any());

        // The call counts: 1 + 1 (running + pending), not 3.
        verify(nodes, times(2)).save(any(ExecutionNode.class));
    }

    @Test
    void cancelRejectsAWaaitingExecutionWithExecutionNotCancelable() {
        UUID orgId = UUID.randomUUID();

        WorkflowExecution execution =
                mock(WorkflowExecution.class);

        when(execution.getId()).thenReturn(UUID.randomUUID());
        when(execution.getOrganizationId()).thenReturn(orgId);
        when(execution.getWorkflowId()).thenReturn(UUID.randomUUID());
        when(execution.getStatus()).thenReturn(ExecutionStatus.WAITING);
        when(execution.getCreatedAt()).thenReturn(Instant.now());

        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        when(executions.findByIdAndOrganizationId(
                execution.getId(),
                orgId))
                .thenReturn(Optional.of(execution));

        ExecutionService svc = service(
                workflows,
                mock(WorkflowVersionRepository.class),
                executions,
                mock(ExecutionNodeRepository.class),
                mock(QueueService.class));

        assertThatThrownBy(() ->
                svc.cancel(
                        principal(orgId, UUID.randomUUID()),
                        execution.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.EXECUTION_NOT_CANCELABLE);

        verify(execution, never()).markCanceled();
    }

    @Test
    void cancelIsANoOpForAnAlreadySucceededRun() {
        UUID orgId = UUID.randomUUID();

        WorkflowExecution execution =
                mock(WorkflowExecution.class);

        when(execution.getId()).thenReturn(UUID.randomUUID());
        when(execution.getOrganizationId()).thenReturn(orgId);
        when(execution.getWorkflowId()).thenReturn(UUID.randomUUID());
        when(execution.getStatus()).thenReturn(ExecutionStatus.SUCCEEDED);
        when(execution.getCreatedAt()).thenReturn(Instant.now());

        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        when(executions.findByIdAndOrganizationId(
                execution.getId(),
                orgId))
                .thenReturn(Optional.of(execution));

        ExecutionNodeRepository nodes =
                mock(ExecutionNodeRepository.class);

        ExecutionService svc = service(
                workflows,
                mock(WorkflowVersionRepository.class),
                executions,
                nodes,
                mock(QueueService.class));

        svc.cancel(
                principal(orgId, UUID.randomUUID()),
                execution.getId());

        verify(execution, never()).markCanceled();

        verify(
                nodes,
                never())
                .findByExecutionIdOrderByCreatedAtAsc(any());
    }

    @Test
    void cancelSurfacesExecutionNotFoundForAnUnknownOrCrossTenantRun() {
        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        when(executions.findByIdAndOrganizationId(id, orgId))
                .thenReturn(Optional.empty());

        ExecutionService svc = service(
                workflows,
                mock(WorkflowVersionRepository.class),
                executions,
                mock(ExecutionNodeRepository.class),
                mock(QueueService.class));

        assertThatThrownBy(() ->
                svc.cancel(
                        principal(orgId, UUID.randomUUID()),
                        id))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.EXECUTION_NOT_FOUND);
    }
}