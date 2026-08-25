package com.flowops.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The per-node state of one run. Created up front as {@code PENDING} for every
 * node in the graph, so the monitor shows the whole plan before it executes.
 *
 * <p>{@code activeHandles} records which output ports fired (comma-joined); the
 * engine reads it to decide which downstream edges activate — and to resume a run
 * from a branch after a human approval, without re-running the node.
 */
@Entity
@Table(name = "execution_nodes")
public class ExecutionNode {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "execution_id", nullable = false, updatable = false)
    private UUID executionId;

    @Column(name = "node_id", nullable = false, updatable = false, length = 128)
    private String nodeId;

    @Column(name = "node_type", nullable = false, updatable = false, length = 64)
    private String nodeType;

    @Column(name = "label", length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NodeRunStatus status;

    @Column(name = "attempt", nullable = false)
    private int attempt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input")
    private JsonNode input;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output")
    private JsonNode output;

    @Column(name = "active_handles", length = 256)
    private String activeHandles;

    @Column(name = "error")
    private String error;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExecutionNode() {
        // JPA
    }

    public static ExecutionNode create(UUID executionId, String nodeId, String nodeType, String label) {
        ExecutionNode node = new ExecutionNode();
        node.id = UUID.randomUUID();
        node.executionId = executionId;
        node.nodeId = nodeId;
        node.nodeType = nodeType;
        node.label = label;
        node.status = NodeRunStatus.PENDING;
        node.attempt = 0;
        Instant now = now();
        node.createdAt = now;
        node.updatedAt = now;
        return node;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private void touch() {
        this.updatedAt = now();
    }

    /** Record the resolved inputs and begin timing this attempt. */
    public void markRunning(JsonNode input) {
        this.status = NodeRunStatus.RUNNING;
        this.input = input;
        this.attempt = this.attempt + 1;
        this.startedAt = now();
        this.error = null;
        touch();
    }

    public void markWaiting() {
        this.status = NodeRunStatus.WAITING;
        touch();
    }

    public void succeed(JsonNode output, List<String> handles) {
        this.status = NodeRunStatus.SUCCEEDED;
        this.output = output;
        this.activeHandles = handles == null ? null : String.join(",", handles);
        finish();
    }

    public void fail(String error) {
        this.status = NodeRunStatus.FAILED;
        this.error = error;
        finish();
    }

    public void skip() {
        this.status = NodeRunStatus.SKIPPED;
        touch();
    }

    /** Clear all run-specific state so the node can execute again on a retry. */
    public void resetForRetry() {
        this.status = NodeRunStatus.PENDING;
        this.input = null;
        this.output = null;
        this.activeHandles = null;
        this.error = null;
        this.startedAt = null;
        this.finishedAt = null;
        this.durationMs = null;
        touch();
    }

    private void finish() {
        this.finishedAt = now();
        if (this.startedAt != null) {
            this.durationMs = this.finishedAt.toEpochMilli() - this.startedAt.toEpochMilli();
        }
        touch();
    }

    public List<String> activeHandles() {
        if (activeHandles == null || activeHandles.isBlank()) {
            return List.of();
        }
        return List.of(activeHandles.split(","));
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getLabel() {
        return label;
    }

    public NodeRunStatus getStatus() {
        return status;
    }

    public int getAttempt() {
        return attempt;
    }

    public JsonNode getInput() {
        return input;
    }

    public JsonNode getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
