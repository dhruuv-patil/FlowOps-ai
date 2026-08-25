package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.domain.ExecutionLog;
import com.flowops.domain.ExecutionNode;
import com.flowops.domain.LogLevel;
import com.flowops.domain.WorkflowExecution;
import com.flowops.repository.ExecutionLogRepository;
import com.flowops.repository.ExecutionNodeRepository;
import com.flowops.repository.WorkflowExecutionRepository;
import com.flowops.repository.WorkflowVersionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The engine's persistence boundary. Each method is its own short transaction, so
 * the {@link ExecutionEngine} — which orchestrates on a worker thread and may sleep
 * for delays or park for approvals — never holds a database connection open across
 * a wait. Every state change the engine makes is committed immediately, so a
 * concurrent monitor read (or the SSE snapshot) always sees live progress.
 *
 * <p>These reads are intentionally <em>not</em> org-scoped: the engine only ever
 * acts on an execution id that {@code ExecutionService} already resolved within the
 * caller's tenant. Tenant isolation lives at the service edge, not here.
 */
@Service
public class ExecutionStore {

    private final WorkflowExecutionRepository executions;
    private final ExecutionNodeRepository nodes;
    private final ExecutionLogRepository logs;
    private final WorkflowVersionRepository versions;

    public ExecutionStore(
            WorkflowExecutionRepository executions,
            ExecutionNodeRepository nodes,
            ExecutionLogRepository logs,
            WorkflowVersionRepository versions) {
        this.executions = executions;
        this.nodes = nodes;
        this.logs = logs;
        this.versions = versions;
    }

    @Transactional(readOnly = true)
    public WorkflowExecution getExecution(UUID executionId) {
        return executions.findById(executionId).orElseThrow(
                () -> new IllegalStateException("Execution vanished mid-run: " + executionId));
    }

    @Transactional(readOnly = true)
    public JsonNode getGraph(UUID versionId) {
        return versions.findById(versionId)
                .orElseThrow(() -> new IllegalStateException("Version vanished mid-run: " + versionId))
                .getGraph();
    }

    @Transactional(readOnly = true)
    public List<ExecutionNode> getNodes(UUID executionId) {
        return nodes.findByExecutionIdOrderByCreatedAtAsc(executionId);
    }

    @Transactional(readOnly = true)
    public int logCount(UUID executionId) {
        return logs.countByExecutionId(executionId);
    }

    @Transactional
    public void saveExecution(WorkflowExecution execution) {
        executions.save(execution);
    }

    @Transactional
    public void saveNode(ExecutionNode node) {
        nodes.save(node);
    }

    @Transactional
    public ExecutionLog appendLog(UUID executionId, String nodeId, LogLevel level, String message, int seq) {
        return logs.save(ExecutionLog.create(executionId, nodeId, level, message, seq));
    }
}
