package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.api.ExecutionLogResponse;
import com.flowops.api.ExecutionNodeResponse;
import com.flowops.api.ExecutionSummaryResponse;
import com.flowops.config.ExecutionProperties;
import com.flowops.domain.ExecutionLog;
import com.flowops.domain.ExecutionNode;
import com.flowops.domain.LogLevel;
import com.flowops.domain.NodeRunStatus;
import com.flowops.domain.WorkflowExecution;
import com.flowops.workflow.graph.GraphDocument;
import com.flowops.workflow.graph.GraphEdge;
import com.flowops.workflow.graph.GraphNode;
import com.flowops.workflow.nodes.NodeDefinition;
import com.flowops.workflow.nodes.NodeRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The heart of M3: drives one execution from {@code QUEUED} to a terminal or
 * {@code WAITING} state, node by node.
 *
 * <p>Runs on a worker thread, deliberately outside any transaction — it may sleep
 * (Delay) or park (Human Approval), and holding a database connection across that
 * would be a leak. All persistence goes through {@link ExecutionStore}, whose
 * methods are each their own short transaction, so every state change is committed
 * the instant it happens and any monitor (or SSE snapshot) sees live progress.
 *
 * <h2>Scheduling</h2>
 * A {@code PENDING} node becomes actionable once every inbound edge's source is
 * resolved (succeeded, failed, or skipped). It then <em>runs</em> if it is a trigger
 * or any inbound edge is active, and is otherwise {@code SKIPPED} (its branch was
 * not taken). An edge is active iff its source succeeded and fired the edge's
 * output port — {@code sourceHandle} names that port ({@code true}/{@code false},
 * {@code approved}/{@code rejected}), null meaning the single default output.
 *
 * <h2>Resume</h2>
 * The engine is re-entrant. On a resume (after an approval or a retry) it reloads
 * committed node state and continues from wherever the graph now stands, reusing
 * every already-succeeded node's output — nothing is recomputed.
 */
@Component
public class ExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEngine.class);

    private final ExecutionStore store;
    private final ExecutionEvents events;
    private final NodeExecutorRegistry executors;
    private final NodeRegistry nodeRegistry;
    private final VariableInterpolator interpolator;
    private final ObjectMapper mapper;
    private final ExecutionProperties properties;

    public ExecutionEngine(
            ExecutionStore store,
            ExecutionEvents events,
            NodeExecutorRegistry executors,
            NodeRegistry nodeRegistry,
            VariableInterpolator interpolator,
            ObjectMapper mapper,
            ExecutionProperties properties) {
        this.store = store;
        this.events = events;
        this.executors = executors;
        this.nodeRegistry = nodeRegistry;
        this.interpolator = interpolator;
        this.mapper = mapper;
        this.properties = properties;
    }

    /** Drives {@code executionId} to a terminal or waiting state. Idempotent on terminal runs. */
    public void run(UUID executionId) {
        WorkflowExecution execution = store.getExecution(executionId);
        if (execution.getStatus().isTerminal()) {
            return;
        }

        GraphDocument graph;
        try {
            graph = mapper.treeToValue(store.getGraph(execution.getWorkflowVersionId()), GraphDocument.class);
        } catch (Exception badGraph) {
            log.error("Execution {} has an unreadable graph", executionId, badGraph);
            fail(execution, "The workflow graph could not be read.", new AtomicInteger(0));
            return;
        }

        Map<String, ExecutionNode> rows = new LinkedHashMap<>();
        for (ExecutionNode node : store.getNodes(executionId)) {
            rows.put(node.getNodeId(), node);
        }
        Map<String, List<GraphEdge>> inbound = indexInbound(graph);
        AtomicInteger seq = new AtomicInteger(store.logCount(executionId));

        boolean fresh = rows.values().stream().allMatch(n -> n.getStatus() == NodeRunStatus.PENDING);
        execution.markRunning();
        store.saveExecution(execution);
        emitExecution(execution);
        logRun(executionId, seq, LogLevel.INFO, fresh ? "Execution started." : "Execution resumed.");

        boolean progressed;
        do {
            progressed = false;
            for (GraphNode node : graph.nodes()) {
                ExecutionNode row = rows.get(node.id());
                if (row == null || row.getStatus() != NodeRunStatus.PENDING) {
                    continue;
                }
                List<GraphEdge> edges = inbound.getOrDefault(node.id(), List.of());
                if (!isTrigger(node)) {
                    if (!allResolved(edges, rows)) {
                        continue;
                    }
                    if (!anyActive(edges, rows)) {
                        skip(executionId, row, seq);
                        progressed = true;
                        continue;
                    }
                }

                NodeResult.Kind outcome = runNode(execution, node, row, rows, seq);
                progressed = true;
                if (outcome == NodeResult.Kind.WAIT) {
                    suspend(execution, seq);
                    return;
                }
                if (outcome == NodeResult.Kind.FAIL) {
                    fail(execution, "Step \"" + label(node, row) + "\" failed: " + safe(row.getError()), seq);
                    return;
                }
            }
        } while (progressed);

        succeed(execution, seq);
    }

    /** Emergency stop from the worker's catch-all: fail the run and any node still running. */
    public void abort(UUID executionId, String reason) {
        WorkflowExecution execution;
        try {
            execution = store.getExecution(executionId);
        } catch (RuntimeException gone) {
            return;
        }
        if (execution.getStatus().isTerminal()) {
            return;
        }
        for (ExecutionNode node : store.getNodes(executionId)) {
            if (node.getStatus() == NodeRunStatus.RUNNING) {
                node.fail(reason);
                store.saveNode(node);
                emitNode(executionId, node);
            }
        }
        fail(execution, reason, new AtomicInteger(store.logCount(executionId)));
    }

    // ---- node execution --------------------------------------------------

    private NodeResult.Kind runNode(
            WorkflowExecution execution,
            GraphNode node,
            ExecutionNode row,
            Map<String, ExecutionNode> rows,
            AtomicInteger seq) {

        UUID executionId = execution.getId();
        Optional<NodeExecutor> executor = executors.find(node.type());
        if (executor.isEmpty()) {
            row.markRunning(mapper.createObjectNode());
            row.fail("No executor is registered for node type \"" + node.type() + "\".");
            store.saveNode(row);
            emitNode(executionId, row);
            return NodeResult.Kind.FAIL;
        }

        JsonNode variables = buildVariables(execution, rows);
        JsonNode input = snapshotInput(node, variables);
        int maxAttempts = properties.maxAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            row.markRunning(input);
            store.saveNode(row);
            emitNode(executionId, row);

            NodeLogger logger = nodeLogger(executionId, node.id(), seq);
            NodeExecutionContext ctx =
                    new NodeExecutionContext(node, variables, interpolator, mapper, properties, logger);

            NodeResult result;
            try {
                result = executor.get().execute(ctx);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                result = NodeResult.fail("The run was interrupted.");
            } catch (Exception failure) {
                result = NodeResult.fail(sanitize(failure));
            }

            switch (result.kind()) {
                case SUCCESS -> {
                    row.succeed(result.output(), fired(node, result.handles()));
                    store.saveNode(row);
                    emitNode(executionId, row);
                    return NodeResult.Kind.SUCCESS;
                }
                case WAIT -> {
                    row.markWaiting();
                    store.saveNode(row);
                    emitNode(executionId, row);
                    return NodeResult.Kind.WAIT;
                }
                case FAIL -> {
                    boolean lastAttempt = attempt >= maxAttempts || Thread.currentThread().isInterrupted();
                    if (lastAttempt) {
                        row.fail(safe(result.error()));
                        store.saveNode(row);
                        emitNode(executionId, row);
                        return NodeResult.Kind.FAIL;
                    }
                    long backoff = backoffMillis(attempt);
                    logger.warn("Attempt " + attempt + " failed: " + safe(result.error())
                            + " Retrying in " + (backoff / 1000.0) + "s.");
                    if (!sleep(backoff)) {
                        row.fail("The run was interrupted.");
                        store.saveNode(row);
                        emitNode(executionId, row);
                        return NodeResult.Kind.FAIL;
                    }
                }
            }
        }
        return NodeResult.Kind.FAIL;
    }

    private void skip(UUID executionId, ExecutionNode row, AtomicInteger seq) {
        row.skip();
        store.saveNode(row);
        emitNode(executionId, row);
        logRun(executionId, seq, LogLevel.DEBUG,
                "Skipped \"" + (row.getLabel() == null ? row.getNodeId() : row.getLabel())
                        + "\" — its branch was not taken.");
    }

    // ---- scheduling helpers ----------------------------------------------

    private Map<String, List<GraphEdge>> indexInbound(GraphDocument graph) {
        Map<String, List<GraphEdge>> inbound = new LinkedHashMap<>();
        for (GraphEdge edge : graph.edges()) {
            if (edge.target() != null) {
                inbound.computeIfAbsent(edge.target(), t -> new ArrayList<>()).add(edge);
            }
        }
        return inbound;
    }

    private boolean isTrigger(GraphNode node) {
        return nodeRegistry.find(node.type()).map(NodeDefinition::trigger).orElse(false);
    }

    private boolean allResolved(List<GraphEdge> inbound, Map<String, ExecutionNode> rows) {
        for (GraphEdge edge : inbound) {
            ExecutionNode source = rows.get(edge.source());
            if (source == null || !source.getStatus().isResolved()) {
                return false;
            }
        }
        return true;
    }

    private boolean anyActive(List<GraphEdge> inbound, Map<String, ExecutionNode> rows) {
        for (GraphEdge edge : inbound) {
            if (isEdgeActive(edge, rows)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEdgeActive(GraphEdge edge, Map<String, ExecutionNode> rows) {
        ExecutionNode source = rows.get(edge.source());
        if (source == null || source.getStatus() != NodeRunStatus.SUCCEEDED) {
            return false;
        }
        String handle = edge.sourceHandle();
        return handle == null || handle.isBlank() || source.activeHandles().contains(handle);
    }

    /** Output ports the node fired; falls back to its declared outputs, else {@code out}. */
    private List<String> fired(GraphNode node, List<String> handles) {
        if (handles != null && !handles.isEmpty()) {
            return handles;
        }
        return nodeRegistry.find(node.type())
                .map(NodeDefinition::outputs)
                .filter(outputs -> !outputs.isEmpty())
                .orElse(List.of("out"));
    }

    // ---- variables & snapshots -------------------------------------------

    private JsonNode buildVariables(WorkflowExecution execution, Map<String, ExecutionNode> rows) {
        ObjectNode root = mapper.createObjectNode();
        JsonNode trigger = execution.getTriggerPayload();
        root.set("trigger", trigger == null || trigger.isNull() ? mapper.createObjectNode() : trigger);

        for (ExecutionNode row : rows.values()) {
            if (row.getStatus() == NodeRunStatus.SUCCEEDED && row.getOutput() != null) {
                root.set(row.getNodeId(), row.getOutput());
            }
        }
        // Type aliases ({{http_request.*}}) for the common single-node-per-type case;
        // never overwrite a node whose id already claimed the key.
        for (ExecutionNode row : rows.values()) {
            if (row.getStatus() == NodeRunStatus.SUCCEEDED && row.getOutput() != null
                    && !root.has(row.getNodeType())) {
                root.set(row.getNodeType(), row.getOutput());
            }
        }
        return root;
    }

    /** Display-only record of the config a node ran with, after interpolation. */
    private JsonNode snapshotInput(GraphNode node, JsonNode variables) {
        if (isTrigger(node)) {
            JsonNode trigger = variables.get("trigger");
            return trigger == null ? mapper.createObjectNode() : trigger.deepCopy();
        }
        ObjectNode snapshot = mapper.createObjectNode();
        node.config().forEach((key, value) -> {
            if (value instanceof String text) {
                snapshot.put(key, interpolator.interpolate(text, variables));
            } else {
                snapshot.set(key, mapper.valueToTree(value));
            }
        });
        return snapshot;
    }

    // ---- terminal transitions --------------------------------------------

    private void succeed(WorkflowExecution execution, AtomicInteger seq) {
        execution.markSucceeded();
        store.saveExecution(execution);
        emitExecution(execution);
        logRun(execution.getId(), seq, LogLevel.INFO, "Execution succeeded.");
        events.complete(execution.getId());
    }

    private void fail(WorkflowExecution execution, String message, AtomicInteger seq) {
        execution.markFailed(safe(message));
        store.saveExecution(execution);
        emitExecution(execution);
        logRun(execution.getId(), seq, LogLevel.ERROR, safe(message));
        events.complete(execution.getId());
    }

    private void suspend(WorkflowExecution execution, AtomicInteger seq) {
        execution.markWaiting();
        store.saveExecution(execution);
        emitExecution(execution);
        logRun(execution.getId(), seq, LogLevel.INFO, "Execution is waiting for a human decision.");
        events.complete(execution.getId());
    }

    // ---- logging & SSE ---------------------------------------------------

    private NodeLogger nodeLogger(UUID executionId, String nodeId, AtomicInteger seq) {
        return new NodeLogger() {
            @Override
            public void debug(String message) {
                write(LogLevel.DEBUG, message);
            }

            @Override
            public void info(String message) {
                write(LogLevel.INFO, message);
            }

            @Override
            public void warn(String message) {
                write(LogLevel.WARN, message);
            }

            @Override
            public void error(String message) {
                write(LogLevel.ERROR, message);
            }

            private void write(LogLevel level, String message) {
                ExecutionLog saved = store.appendLog(executionId, nodeId, level, safe(message), seq.getAndIncrement());
                events.emit(executionId, ExecutionEvents.LOG, ExecutionLogResponse.of(saved));
            }
        };
    }

    private void logRun(UUID executionId, AtomicInteger seq, LogLevel level, String message) {
        ExecutionLog saved = store.appendLog(executionId, null, level, safe(message), seq.getAndIncrement());
        events.emit(executionId, ExecutionEvents.LOG, ExecutionLogResponse.of(saved));
    }

    private void emitNode(UUID executionId, ExecutionNode node) {
        events.emit(executionId, ExecutionEvents.NODE, ExecutionNodeResponse.of(node));
    }

    private void emitExecution(WorkflowExecution execution) {
        events.emit(execution.getId(), ExecutionEvents.EXECUTION,
                ExecutionSummaryResponse.of(execution, null));
    }

    // ---- misc ------------------------------------------------------------

    private long backoffMillis(int attempt) {
        long base = properties.retryBackoff().toMillis();
        long scaled = base * (1L << (attempt - 1));
        return Math.min(scaled, properties.maxRetryBackoff().toMillis());
    }

    private boolean sleep(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String label(GraphNode node, ExecutionNode row) {
        if (row.getLabel() != null && !row.getLabel().isBlank()) {
            return row.getLabel();
        }
        return node.id();
    }

    /** Keeps messages single-line, bounded, and non-null for a user-safe log/error. */
    private String safe(String message) {
        if (message == null || message.isBlank()) {
            return "Unexpected error.";
        }
        String cleaned = message.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 500 ? cleaned.substring(0, 500) + "…" : cleaned;
    }

    private String sanitize(Exception failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }
}
