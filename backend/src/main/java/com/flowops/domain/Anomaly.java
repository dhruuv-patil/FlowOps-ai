package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A detected reliability anomaly (reliability pivot, M3).
 *
 * <p>Produced by a statistical/rule-based detector — never by the LLM — when a fresh
 * observation deviates from the learned {@link MetricBaseline}. It is an explainable
 * record: {@code expectedValue}/{@code actualValue}/{@code deviation}/{@code confidence}
 * plus a structured {@code evidence} map carrying the severity rationale. It never holds
 * payload values or secrets — only sizes, structure, and aggregates.
 *
 * <p>Repeated occurrences of the same underlying problem aggregate into a single record
 * via {@code dedupKey} ({@link #aggregate}), bumping {@code affectedExecutions} instead
 * of spamming the dashboard with a row per run.
 */
@Entity
@Table(name = "anomalies")
public class Anomaly {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    /** Null for a workflow-scoped anomaly (e.g. volume). */
    @Column(name = "node_id", length = 128)
    private String nodeId;

    /** The run that triggered detection; null once that run is deleted (ON DELETE SET NULL). */
    @Column(name = "execution_id")
    private UUID executionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private AnomalyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private AnomalySeverity severity;

    @Column(name = "metric", length = 64)
    private String metric;

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(name = "actual_value")
    private String actualValue;

    @Column(name = "deviation")
    private Double deviation;

    @Column(name = "confidence")
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnomalyStatus status;

    @Column(name = "affected_executions", nullable = false)
    private int affectedExecutions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence")
    private Map<String, Object> evidence;

    @Column(name = "dedup_key", nullable = false, length = 128)
    private String dedupKey;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Anomaly() {
        // JPA
    }

    /**
     * Opens a fresh anomaly. {@code affectedExecutions} starts at 1; subsequent repeats
     * of the same {@code dedupKey} are folded in with {@link #aggregate}.
     */
    public static Anomaly open(
            UUID organizationId,
            UUID workflowId,
            String nodeId,
            UUID executionId,
            AnomalyType type,
            AnomalySeverity severity,
            String metric,
            String expectedValue,
            String actualValue,
            double deviation,
            double confidence,
            Map<String, Object> evidence,
            String dedupKey) {
        Anomaly a = new Anomaly();
        a.id = UUID.randomUUID();
        a.organizationId = organizationId;
        a.workflowId = workflowId;
        a.nodeId = nodeId;
        a.executionId = executionId;
        a.type = type;
        a.severity = severity;
        a.metric = metric;
        a.expectedValue = expectedValue;
        a.actualValue = actualValue;
        a.deviation = deviation;
        a.confidence = confidence;
        a.evidence = evidence;
        a.dedupKey = dedupKey;
        a.status = AnomalyStatus.OPEN;
        a.affectedExecutions = 1;
        Instant now = now();
        a.detectedAt = now;
        a.updatedAt = now;
        a.createdAt = now;
        return a;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private void touch() {
        this.updatedAt = now();
    }

    /**
     * Folds a repeat occurrence into this record: bumps the affected-execution count,
     * refreshes the observed value/deviation/confidence/evidence and (only ever upward)
     * the severity. Never lowers severity — an anomaly that once looked CRITICAL stays
     * at least visible at that band until a human resolves it.
     */
    public void aggregate(
            AnomalySeverity newSeverity,
            String actualValue,
            double deviation,
            double confidence,
            UUID executionId,
            Map<String, Object> evidence) {
        this.affectedExecutions += 1;
        this.actualValue = actualValue;
        this.deviation = deviation;
        this.confidence = confidence;
        this.evidence = evidence;
        if (executionId != null) {
            this.executionId = executionId;
        }
        if (newSeverity != null && newSeverity.ordinal() > this.severity.ordinal()) {
            this.severity = newSeverity;
        }
        // A repeat is fresh evidence the problem is on-going: reopen if a human had
        // acknowledged it (but respect an explicit RESOLVED/FALSE_POSITIVE decision —
        // the recorder gates those out before calling aggregate).
        if (this.status == AnomalyStatus.ACKNOWLEDGED) {
            this.status = AnomalyStatus.OPEN;
        }
        touch();
    }

    /**
     * Starts a fresh episode on this record after a quiet cooldown gap: the same defect
     * recurred, so keep one card but reset the episode ({@code detected_at},
     * {@code affected_executions}) rather than spawn a lookalike row.
     */
    public void newEpisode(
            AnomalySeverity severity,
            String actualValue,
            double deviation,
            double confidence,
            UUID executionId,
            Map<String, Object> evidence) {
        this.affectedExecutions = 1;
        this.severity = severity;
        this.actualValue = actualValue;
        this.deviation = deviation;
        this.confidence = confidence;
        this.executionId = executionId;
        this.evidence = evidence;
        this.status = AnomalyStatus.OPEN;
        this.detectedAt = now();
        touch();
    }

    public void acknowledge() {
        this.status = AnomalyStatus.ACKNOWLEDGED;
        touch();
    }

    public void resolve() {
        this.status = AnomalyStatus.RESOLVED;
        touch();
    }

    public void markFalsePositive() {
        this.status = AnomalyStatus.FALSE_POSITIVE;
        touch();
    }

    // ---- getters ----------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public AnomalyType getType() {
        return type;
    }

    public AnomalySeverity getSeverity() {
        return severity;
    }

    public String getMetric() {
        return metric;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public Double getDeviation() {
        return deviation;
    }

    public Double getConfidence() {
        return confidence;
    }

    public AnomalyStatus getStatus() {
        return status;
    }

    public int getAffectedExecutions() {
        return affectedExecutions;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
        touch();
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
