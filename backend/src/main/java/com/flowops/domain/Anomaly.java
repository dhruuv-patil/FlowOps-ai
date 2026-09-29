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

    // ---- recovery verification fields ----------------------------------------

    /**
     * When recovery verification was started (null = never started).
     * Only set when status transitions to VERIFYING_RECOVERY.
     */
    @Column(name = "recovery_started_at")
    private Instant recoveryStartedAt;

    /**
     * Number of healthy (non-anomalous) executions observed since verification started.
     */
    @Column(name = "recovery_healthy_count", nullable = false)
    private int recoveryHealthyCount;

    /**
     * Total executions observed during the verification window (healthy + unhealthy).
     */
    @Column(name = "recovery_observed_count", nullable = false)
    private int recoveryObservedCount;

    /**
     * How many consecutive healthy executions are required to confirm recovery.
     * Defaults to 5; may be overridden at verification-start time.
     */
    @Column(name = "recovery_required_count", nullable = false)
    private int recoveryRequiredCount;

    /**
     * Snapshot of the execution IDs already counted, serialized as a JSON array.
     * Prevents double-counting the same execution across multiple telemetry rows.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recovery_seen_executions")
    private java.util.Set<UUID> recoverySeenExecutions;

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

    /**
     * Begins recovery verification. The anomaly enters {@link AnomalyStatus#VERIFYING_RECOVERY};
     * subsequent executions will be evaluated against the same baseline to confirm the fix.
     *
     * @param requiredCount  healthy executions needed to confirm recovery
     */
    public void startRecoveryVerification(int requiredCount) {
        this.status = AnomalyStatus.VERIFYING_RECOVERY;
        this.recoveryStartedAt = now();
        this.recoveryHealthyCount = 0;
        this.recoveryObservedCount = 0;
        this.recoveryRequiredCount = Math.max(1, requiredCount);
        this.recoverySeenExecutions = new java.util.LinkedHashSet<>();
        touch();
    }

    /**
     * Records one more execution observed during the recovery window.
     *
     * @param executionId  the execution that just completed
     * @param healthy      true when the metric was within the normal baseline range
     * @return {@code true} if recovery is now confirmed (enough healthy executions)
     */
    public boolean recordRecoveryExecution(UUID executionId, boolean healthy) {
        if (this.recoverySeenExecutions == null) {
            this.recoverySeenExecutions = new java.util.LinkedHashSet<>();
        }
        // Idempotent: do not count the same execution twice
        if (!this.recoverySeenExecutions.add(executionId)) {
            return false;
        }
        this.recoveryObservedCount += 1;
        if (healthy) {
            this.recoveryHealthyCount += 1;
        } else {
            // An unhealthy observation resets the healthy streak — the metric is still
            // anomalous. Keep the observed count but zero the healthy counter so
            // the user can see "2/5 — metric still outside range" rather than a
            // misleadingly high count.
            this.recoveryHealthyCount = 0;
        }
        touch();
        boolean confirmed = this.recoveryHealthyCount >= this.recoveryRequiredCount;
        if (confirmed) {
            this.status = AnomalyStatus.RESOLVED;
        }
        return confirmed;
    }

    /**
     * Resets recovery verification back to {@link AnomalyStatus#OPEN} when the
     * monitoring window closes without enough healthy evidence.
     */
    public void cancelRecoveryVerification() {
        if (this.status == AnomalyStatus.VERIFYING_RECOVERY) {
            this.status = AnomalyStatus.OPEN;
            touch();
        }
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

    // ---- recovery verification getters ---------------------------------------

    public Instant getRecoveryStartedAt() {
        return recoveryStartedAt;
    }

    public int getRecoveryHealthyCount() {
        return recoveryHealthyCount;
    }

    public int getRecoveryObservedCount() {
        return recoveryObservedCount;
    }

    public int getRecoveryRequiredCount() {
        return recoveryRequiredCount;
    }

    public java.util.Set<UUID> getRecoverySeenExecutions() {
        return recoverySeenExecutions;
    }
}
