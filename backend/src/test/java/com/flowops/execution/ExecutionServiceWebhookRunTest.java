package com.flowops.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.ExecutionNode;
import com.flowops.domain.TriggerType;
import com.flowops.domain.Workflow;
import com.flowops.domain.WorkflowExecution;
import com.flowops.domain.WorkflowVersion;
import com.flowops.repository.ExecutionLogRepository;
import com.flowops.repository.ExecutionNodeRepository;
import com.flowops.repository.IdempotencyKeyRepository;
import com.flowops.repository.WorkflowExecutionRepository;
import com.flowops.repository.WorkflowRepository;
import com.flowops.repository.WorkflowVersionRepository;
import com.flowops.workflow.nodes.NodeRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Slice test for {@link ExecutionService#startWebhookRun}, the principal-free run path
 * behind inbound webhooks. Repositories and the queue are mocked; a real
 * {@link NodeRegistry} and {@link ObjectMapper} parse the graph exactly as production.
 *
 * <p>The invariants under test: a webhook run is created with {@code created_by == null}
 * and {@code trigger_type == WEBHOOK}, one {@code PENDING} node row per graph node is
 * persisted, and every not-runnable condition (no published version, or a graph without
 * a webhook trigger) surfaces as the same opaque {@link ErrorCode#NOT_FOUND}.
 */
class ExecutionServiceWebhookRunTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // A real properties record (it is final and cannot be mocked); startWebhookRun never
    // reads it, but the constructor requires an instance.
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

    private JsonNode graph(String... nodeTypes) {
        var root = MAPPER.createObjectNode();
        var nodes = root.putArray("nodes");

        int i = 0;

        for (String type : nodeTypes) {
            var node = nodes.addObject();
            node.put("id", "n" + i++);
            node.put("type", type);
        }

        root.putArray("edges");

        return root;
    }

    private Workflow publishedWorkflow(UUID orgId) {
        Workflow workflow = Workflow.create(
                orgId,
                UUID.randomUUID(),
                "Wf",
                null,
                MAPPER.createObjectNode());

        workflow.markPublished(1);

        return workflow;
    }

    private WorkflowVersion versionWith(JsonNode graph) {
        WorkflowVersion version = mock(WorkflowVersion.class);

        when(version.getId()).thenReturn(UUID.randomUUID());
        when(version.getGraph()).thenReturn(graph);

        return version;
    }

    @Test
    void startWebhookRunCreatesAPrincipalFreeWebhookRunWithANodePerGraphNode() {
        UUID orgId = UUID.randomUUID();

        Workflow workflow = publishedWorkflow(orgId);

        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        WorkflowVersionRepository versions =
                mock(WorkflowVersionRepository.class);

        WorkflowExecutionRepository executions =
                mock(WorkflowExecutionRepository.class);

        ExecutionNodeRepository nodes =
                mock(ExecutionNodeRepository.class);

        QueueService queue =
                mock(QueueService.class);

        // Build the version stub-target BEFORE the enclosing when(...) — versionWith()
        // itself calls when(...), which would otherwise start a new stubbing inside the
        // outer one and trip UnfinishedStubbingException.
        WorkflowVersion version =
                versionWith(graph("webhook_trigger", "notification"));

        when(workflows.findByIdAndOrganizationId(
                workflow.getId(),
                orgId))
                .thenReturn(Optional.of(workflow));

        when(versions.findByWorkflowIdAndVersionNumber(
                workflow.getId(),
                1))
                .thenReturn(Optional.of(version));

        JsonNode payload =
                MAPPER.createObjectNode()
                        .put("hello", "world");

        UUID executionId =
                service(
                        workflows,
                        versions,
                        executions,
                        nodes,
                        queue)
                        .startWebhookRun(
                                workflow.getId(),
                                orgId,
                                payload);

        ArgumentCaptor<WorkflowExecution> savedRun =
                ArgumentCaptor.forClass(WorkflowExecution.class);

        verify(executions).save(savedRun.capture());

        WorkflowExecution run =
                savedRun.getValue();

        assertThat(run.getId())
                .isEqualTo(executionId);

        assertThat(run.getCreatedBy())
                .isNull();

        assertThat(run.getTriggerType())
                .isEqualTo(TriggerType.WEBHOOK);

        assertThat(run.getOrganizationId())
                .isEqualTo(orgId);

        // One PENDING node row per graph node; the run is enqueued once.
        verify(nodes, times(2))
                .save(any(ExecutionNode.class));

        verify(queue)
                .enqueue(executionId);
    }

    @Test
    void startWebhookRunIsOpaqueWhenTheWorkflowIsNotPublished() {
        UUID orgId = UUID.randomUUID();

        Workflow draft =
                Workflow.create(
                        orgId,
                        UUID.randomUUID(),
                        "Wf",
                        null,
                        MAPPER.createObjectNode());

        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        when(workflows.findByIdAndOrganizationId(
                draft.getId(),
                orgId))
                .thenReturn(Optional.of(draft));

        ExecutionService service =
                service(
                        workflows,
                        mock(WorkflowVersionRepository.class),
                        mock(WorkflowExecutionRepository.class),
                        mock(ExecutionNodeRepository.class),
                        mock(QueueService.class));

        assertThatThrownBy(() ->
                service.startWebhookRun(
                        draft.getId(),
                        orgId,
                        MAPPER.createObjectNode()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void startWebhookRunIsOpaqueWhenTheGraphHasNoWebhookTrigger() {
        UUID orgId = UUID.randomUUID();

        Workflow workflow =
                publishedWorkflow(orgId);

        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        WorkflowVersionRepository versions =
                mock(WorkflowVersionRepository.class);

        when(workflows.findByIdAndOrganizationId(
                workflow.getId(),
                orgId))
                .thenReturn(Optional.of(workflow));

        // Hoisted out of the when(...) below for the same reason as above.
        WorkflowVersion version =
                versionWith(
                        graph(
                                "manual_trigger",
                                "notification"));

        when(versions.findByWorkflowIdAndVersionNumber(
                workflow.getId(),
                1))
                .thenReturn(Optional.of(version));

        ExecutionService service =
                service(
                        workflows,
                        versions,
                        mock(WorkflowExecutionRepository.class),
                        mock(ExecutionNodeRepository.class),
                        mock(QueueService.class));

        assertThatThrownBy(() ->
                service.startWebhookRun(
                        workflow.getId(),
                        orgId,
                        MAPPER.createObjectNode()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void startWebhookRunIsOpaqueForAnUnknownOrCrossTenantWorkflow() {
        UUID orgId = UUID.randomUUID();

        WorkflowRepository workflows =
                mock(WorkflowRepository.class);

        when(workflows.findByIdAndOrganizationId(
                any(),
                any()))
                .thenReturn(Optional.empty());

        ExecutionService service =
                service(
                        workflows,
                        mock(WorkflowVersionRepository.class),
                        mock(WorkflowExecutionRepository.class),
                        mock(ExecutionNodeRepository.class),
                        mock(QueueService.class));

        assertThatThrownBy(() ->
                service.startWebhookRun(
                        UUID.randomUUID(),
                        orgId,
                        MAPPER.createObjectNode()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}