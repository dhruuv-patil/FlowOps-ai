package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * One append-only line in a run's log. {@code nodeId} is null for execution-level
 * lines (run started, finished). {@code seq} is a per-run monotonic counter set by
 * the engine so the timeline stays stable when several lines share a millisecond.
 *
 * <p>Contract §9: no password, token, or secret is ever written here — executors
 * log outcomes and user-safe detail, never raw credentials or full response bodies.
 */
@Entity
@Table(name = "execution_logs")
public class ExecutionLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "execution_id", nullable = false, updatable = false)
    private UUID executionId;

    @Column(name = "node_id", length = 128, updatable = false)
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 8, updatable = false)
    private LogLevel level;

    @Column(name = "message", nullable = false, updatable = false)
    private String message;

    @Column(name = "seq", nullable = false, updatable = false)
    private int seq;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExecutionLog() {
        // JPA
    }

    public static ExecutionLog create(
            UUID executionId, String nodeId, LogLevel level, String message, int seq) {
        ExecutionLog log = new ExecutionLog();
        log.id = UUID.randomUUID();
        log.executionId = executionId;
        log.nodeId = nodeId;
        log.level = level;
        log.message = message;
        log.seq = seq;
        log.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return log;
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

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public int getSeq() {
        return seq;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
