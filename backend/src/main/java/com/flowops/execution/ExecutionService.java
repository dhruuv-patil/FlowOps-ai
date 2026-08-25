package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.api.ExecutionDetailResponse;
import com.flowops.api.ExecutionEnvelopes;
import com.flowops.api.ExecutionLogResponse;
import com.flowops.api.ExecutionNodeResponse;
import com.flowops.api.ExecutionStatsResponse;
import com.flowops.api.ExecutionSummaryResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.ExecutionLog;
import com.flowops.domain.ExecutionNode;
import com.flowops.domain.ExecutionStatus;
import com.flowops.domain.LogLevel;
import com.flowops.domain.NodeRunStatus;
import com.flowops.domain.TriggerType;
import com.flowops.domain.Workflow;
import com.flowops.domain.WorkflowExecution;
import com.flowops.domain.WorkflowVersion;
import com.flowops.repository.ExecutionLogRepository;
import com.flowops.repository.ExecutionNodeRepository;
import com.flowops.repository.ExecutionStatRow;
import com.flowops.repository.WorkflowExecutionRepository;
import com.flowops.repository.WorkflowRepository;
import com.flowops.repository.WorkflowVersionRepository;
import com.flowops.config.ExecutionProperties;
import com.flowops.security.FlowOpsPrincipal;
import com.flowops.workflow.graph.GraphDocument;
import com.flowops.workflow.graph.GraphNode;
import com.flowops.workflow.nodes.NodeDefinition;
import com.flowops.workflow.nodes.NodeRegistry;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Request-side of the execution engine: starting runs, listing and reading them,
 * resolving approvals, retrying failures, streaming live progress, and dashboard
 * analytics.
 *
 * <p>Every entry point derives the tenant from the {@link FlowOpsPrincipal} and
 * funnels reads through {@link #require} — an execution owned by another
 * organization is indistinguishable from one that does not exist
 * ({@link ErrorCode#EXECUTION_NOT_FOUND}, contract §2 anti-enumeration). The engine
 * itself only ever receives an id already resolved within the caller's org.
 */
@Service
public class ExecutionService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final WorkflowRepository workflows;
    private final WorkflowVersionRepository versions;
    private final WorkflowExecutionRepository executions;
    private final ExecutionNodeRepository nodes;
    private final ExecutionLogRepository logs;
    private final QueueService queue;
    private final ExecutionEvents events;
    private final ExecutionProperties properties;
    private final NodeRegistry nodeRegistry;
    private final ObjectMapper mapper;

    public ExecutionService(
            WorkflowRepository workflows,
            WorkflowVersionRepository versions,
            WorkflowExecutionRepository executions,
            ExecutionNodeRepository nodes,
            ExecutionLogRepository logs,
            QueueService queue,
            ExecutionEvents events,
            ExecutionProperties properties,
            NodeRegistry nodeRegistry,
            ObjectMapper mapper) {
        this.workflows = workflows;
        this.versions = versions;
        this.executions = executions;
        this.nodes = nodes;
        this.logs = logs;
        this.queue = queue;
        this.events = events;
        this.properties = properties;
        this.nodeRegistry = nodeRegistry;
        this.mapper = mapper;
    }

    // ---- start -----------------------------------------------------------

    /**
     * Starts a run of a workflow's latest published version. Creates the run and a
     * {@code PENDING} row per graph node, then enqueues it — after the transaction
     * commits, so the worker never races ahead of the rows it needs to read.
     */
    @Transactional
    public ExecutionDetailResponse run(
            FlowOpsPrincipal principal, UUID workflowId, RunWorkflowRequest request) {
        Workflow workflow = workflows
                .findByIdAndOrganizationId(workflowId, principal.organizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.WORKFLOW_NOT_FOUND));

        Integer latest = workflow.getLatestVersion();
        if (latest == null) {
            throw new ApiException(ErrorCode.WORKFLOW_NOT_PUBLISHED);
        }
        WorkflowVersion version = versions
                .findByWorkflowIdAndVersionNumber(workflowId, latest)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKFLOW_NOT_PUBLISHED));

        GraphDocument graph = parse(version.getGraph());
        JsonNode payload = (request == null || request.input() == null || request.input().isNull())
                ? mapper.createObjectNode()
                : request.input();

        WorkflowExecution execution = WorkflowExecution.create(
                principal.organizationId(),
                workflowId,
                version.getId(),
                latest,
                triggerTypeOf(graph),
                payload,
                principal.userId());
        executions.save(execution);

        for (GraphNode node : graph.nodes()) {
            nodes.save(ExecutionNode.create(execution.getId(), node.id(), node.type(), labelOf(node)));
        }

        enqueueAfterCommit(execution.getId());
        return detail(execution, workflow.getName());
    }

    // ---- read ------------------------------------------------------------

    @Transactional(readOnly = true)
    public ExecutionEnvelopes.Executions list(
            FlowOpsPrincipal principal, UUID workflowId, ExecutionStatus status, Integer limit) {
        List<WorkflowExecution> rows = executions.search(
                principal.organizationId(), workflowId, status, PageRequest.of(0, clampLimit(limit)));

        Map<UUID, String> names = new HashMap<>();
        List<ExecutionSummaryResponse> summaries = new ArrayList<>(rows.size());
        for (WorkflowExecution execution : rows) {
            String name = names.computeIfAbsent(
                    execution.getWorkflowId(),
                    id -> workflowName(principal.organizationId(), id));
            summaries.add(ExecutionSummaryResponse.of(execution, name));
        }
        return new ExecutionEnvelopes.Executions(summaries);
    }

    @Transactional(readOnly = true)
    public ExecutionDetailResponse get(FlowOpsPrincipal principal, UUID executionId) {
        WorkflowExecution execution = require(principal, executionId);
        return detail(execution, workflowName(principal.organizationId(), execution.getWorkflowId()));
    }

    /**
     * Opens a live SSE stream for a run. Ownership is checked first; then the caller
     * gets an immediate snapshot (current run status, every node, the log so far)
     * followed by live deltas. A run that is not actively progressing is closed
     * right after the snapshot — the client re-opens the stream if it resumes.
     */
    @Transactional(readOnly = true)
    public SseEmitter stream(FlowOpsPrincipal principal, UUID executionId) {
        WorkflowExecution execution = require(principal, executionId);
        SseEmitter emitter = events.subscribe(executionId, properties.sseTimeout().toMillis());
        try {
            emitter.send(SseEmitter.event()
                    .name(ExecutionEvents.EXECUTION)
                    .data(ExecutionSummaryResponse.of(execution, null)));
            for (ExecutionNode node : nodes.findByExecutionIdOrderByCreatedAtAsc(executionId)) {
                emitter.send(SseEmitter.event().name(ExecutionEvents.NODE).data(ExecutionNodeResponse.of(node)));
            }
            for (ExecutionLog entry : logs.findByExecutionIdOrderBySeqAsc(executionId)) {
                emitter.send(SseEmitter.event().name(ExecutionEvents.LOG).data(ExecutionLogResponse.of(entry)));
            }
            ExecutionStatus status = execution.getStatus();
            if (status != ExecutionStatus.RUNNING && status != ExecutionStatus.QUEUED) {
                emitter.send(SseEmitter.event().name(ExecutionEvents.DONE).data("{}"));
                emitter.complete();
            }
        } catch (Exception disconnected) {
            emitter.completeWithError(disconnected);
        }
        return emitter;
    }

    // ---- transitions -----------------------------------------------------

    /** Resolves a waiting Human Approval node and resumes the run down the chosen branch. */
    @Transactional
    public ExecutionDetailResponse decide(
            FlowOpsPrincipal principal, UUID executionId, String nodeId, ApprovalDecisionRequest request) {
        WorkflowExecution execution = require(principal, executionId);
        if (execution.getStatus() != ExecutionStatus.WAITING) {
            throw new ApiException(ErrorCode.APPROVAL_NOT_PENDING);
        }
        ExecutionNode node = nodes.findByExecutionIdAndNodeId(executionId, nodeId)
                .orElseThrow(() -> new ApiException(ErrorCode.APPROVAL_NOT_PENDING));
        if (!"human_approval".equals(node.getNodeType()) || node.getStatus() != NodeRunStatus.WAITING) {
            throw new ApiException(ErrorCode.APPROVAL_NOT_PENDING);
        }

        boolean approved = Boolean.TRUE.equals(request.approved());
        String handle = approved ? "approved" : "rejected";

        ObjectNode output = mapper.createObjectNode();
        output.put("approved", approved);
        output.put("decidedBy", principal.userId().toString());
        if (request.note() != null && !request.note().isBlank()) {
            output.put("note", request.note().strip());
        }
        node.succeed(output, List.of(handle));
        nodes.save(node);

        int seq = logs.countByExecutionId(executionId);
        String note = (request.note() == null || request.note().isBlank()) ? "" : ": " + request.note().strip();
        logs.save(ExecutionLog.create(executionId, nodeId, LogLevel.INFO,
                "Approval " + (approved ? "granted" : "rejected") + note + ".", seq));

        execution.reopenForRetry();
        executions.save(execution);

        enqueueAfterCommit(executionId);
        return detail(execution, workflowName(principal.organizationId(), execution.getWorkflowId()));
    }

    /** Re-runs a failed execution from its failed step, reusing already-succeeded outputs. */
    @Transactional
    public ExecutionDetailResponse retry(FlowOpsPrincipal principal, UUID executionId) {
        WorkflowExecution execution = require(principal, executionId);
        if (execution.getStatus() != ExecutionStatus.FAILED) {
            throw new ApiException(ErrorCode.EXECUTION_NOT_RETRYABLE);
        }
        for (ExecutionNode node : nodes.findByExecutionIdOrderByCreatedAtAsc(executionId)) {
            if (node.getStatus() != NodeRunStatus.SUCCEEDED) {
                node.resetForRetry();
                nodes.save(node);
            }
        }
        int seq = logs.countByExecutionId(executionId);
        logs.save(ExecutionLog.create(executionId, null, LogLevel.INFO, "Retrying the failed run.", seq));

        execution.reopenForRetry();
        executions.save(execution);

        enqueueAfterCommit(executionId);
        return detail(execution, workflowName(principal.organizationId(), execution.getWorkflowId()));
    }

    // ---- analytics -------------------------------------------------------

    @Transactional(readOnly = true)
    public ExecutionStatsResponse stats(FlowOpsPrincipal principal, String rangeParam) {
        Range range = Range.parse(rangeParam);
        Instant now = Instant.now();
        Instant start = now.truncatedTo(range.unit).minus((range.buckets - 1L) * range.stepSeconds, ChronoUnit.SECONDS);

        List<ExecutionStatRow> rows = executions.statsSince(principal.organizationId(), start);
        return new ExecutionStatsResponse(range.label, start, totals(rows), series(rows, range, start));
    }

    private ExecutionStatsResponse.Totals totals(List<ExecutionStatRow> rows) {
        long succeeded = 0;
        long failed = 0;
        long running = 0;
        long waiting = 0;
        long queued = 0;
        long canceled = 0;
        long durationSum = 0;
        long durationCount = 0;

        for (ExecutionStatRow row : rows) {
            switch (row.getStatus()) {
                case SUCCEEDED -> succeeded++;
                case FAILED -> failed++;
                case RUNNING -> running++;
                case WAITING -> waiting++;
                case QUEUED -> queued++;
                case CANCELED -> canceled++;
            }
            if (row.getStartedAt() != null && row.getFinishedAt() != null) {
                durationSum += Duration.between(row.getStartedAt(), row.getFinishedAt()).toMillis();
                durationCount++;
            }
        }
        long decided = succeeded + failed;
        double successRate = decided == 0 ? 0.0 : (double) succeeded / decided;
        Long avg = durationCount == 0 ? null : durationSum / durationCount;
        return new ExecutionStatsResponse.Totals(
                rows.size(), succeeded, failed, running, waiting, queued, canceled, successRate, avg);
    }

    private List<ExecutionStatsResponse.Point> series(
            List<ExecutionStatRow> rows, Range range, Instant start) {
        long[] total = new long[range.buckets];
        long[] succeeded = new long[range.buckets];
        long[] failed = new long[range.buckets];
        long stepMillis = range.stepSeconds * 1000L;

        for (ExecutionStatRow row : rows) {
            if (row.getCreatedAt() == null || row.getCreatedAt().isBefore(start)) {
                continue;
            }
            int index = (int) (Duration.between(start, row.getCreatedAt()).toMillis() / stepMillis);
            if (index < 0 || index >= range.buckets) {
                continue;
            }
            total[index]++;
            if (row.getStatus() == ExecutionStatus.SUCCEEDED) {
                succeeded[index]++;
            } else if (row.getStatus() == ExecutionStatus.FAILED) {
                failed[index]++;
            }
        }

        List<ExecutionStatsResponse.Point> points = new ArrayList<>(range.buckets);
        for (int i = 0; i < range.buckets; i++) {
            Instant bucketStart = start.plus((long) i * range.stepSeconds, ChronoUnit.SECONDS);
            points.add(new ExecutionStatsResponse.Point(bucketStart, total[i], succeeded[i], failed[i]));
        }
        return points;
    }

    /** The dashboard time windows, each a fixed number of equal buckets. */
    private enum Range {
        H24(24, 3600, ChronoUnit.HOURS, "24h"),
        D7(7, 86_400, ChronoUnit.DAYS, "7d"),
        D30(30, 86_400, ChronoUnit.DAYS, "30d"),
        D90(90, 86_400, ChronoUnit.DAYS, "90d");

        private final int buckets;
        private final long stepSeconds;
        private final ChronoUnit unit;
        private final String label;

        Range(int buckets, long stepSeconds, ChronoUnit unit, String label) {
            this.buckets = buckets;
            this.stepSeconds = stepSeconds;
            this.unit = unit;
            this.label = label;
        }

        static Range parse(String value) {
            if (value == null) {
                return D7;
            }
            return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "24h" -> H24;
                case "30d" -> D30;
                case "90d" -> D90;
                default -> D7;
            };
        }
    }

    // ---- helpers ---------------------------------------------------------

    /** Fetch scoped to the caller's org, or 404. The single choke point for tenant isolation. */
    private WorkflowExecution require(FlowOpsPrincipal principal, UUID executionId) {
        return executions
                .findByIdAndOrganizationId(executionId, principal.organizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.EXECUTION_NOT_FOUND));
    }

    private ExecutionDetailResponse detail(WorkflowExecution execution, String workflowName) {
        List<ExecutionNodeResponse> nodeDtos = nodes
                .findByExecutionIdOrderByCreatedAtAsc(execution.getId())
                .stream().map(ExecutionNodeResponse::of).toList();
        List<ExecutionLogResponse> logDtos = logs
                .findByExecutionIdOrderBySeqAsc(execution.getId())
                .stream().map(ExecutionLogResponse::of).toList();
        return ExecutionDetailResponse.of(execution, workflowName, nodeDtos, logDtos);
    }

    private String workflowName(UUID organizationId, UUID workflowId) {
        return workflows.findByIdAndOrganizationId(workflowId, organizationId)
                .map(Workflow::getName)
                .orElse(null);
    }

    private GraphDocument parse(JsonNode graph) {
        try {
            return mapper.treeToValue(graph, GraphDocument.class);
        } catch (Exception unreadable) {
            throw new ApiException(ErrorCode.WORKFLOW_INVALID);
        }
    }

    private TriggerType triggerTypeOf(GraphDocument graph) {
        for (GraphNode node : graph.nodes()) {
            boolean isTrigger = nodeRegistry.find(node.type())
                    .map(NodeDefinition::trigger)
                    .orElse(false);
            if (isTrigger) {
                return "webhook_trigger".equals(node.type()) ? TriggerType.WEBHOOK : TriggerType.MANUAL;
            }
        }
        return TriggerType.MANUAL;
    }

    private String labelOf(GraphNode node) {
        Object label = node.data().get("label");
        if (label instanceof String text && !text.isBlank()) {
            return text;
        }
        return nodeRegistry.find(node.type()).map(NodeDefinition::label).orElse(node.type());
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * Enqueue only once the surrounding transaction commits, so the worker thread
     * cannot read a half-written run. Falls back to an immediate enqueue if somehow
     * called without an active transaction.
     */
    private void enqueueAfterCommit(UUID executionId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    queue.enqueue(executionId);
                }
            });
        } else {
            queue.enqueue(executionId);
        }
    }
}
