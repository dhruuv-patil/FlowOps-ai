package com.flowops.domain;

import com.flowops.reliability.baseline.RobustStats;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A learned statistical baseline for one (workflow, node, metric) key (reliability
 * pivot). Summarizes the "normal" range for a metric over a rolling window so anomaly
 * detectors can compare fresh observations against it.
 *
 * <ul>
 *   <li>{@code LATENCY}/{@code OUTPUT_SIZE}/{@code RETRY_RATE} — robust numeric summary
 *       (median, MAD, percentiles) computed from per-node telemetry in the window.
 *   <li>{@code ERROR_RATE}/{@code VOLUME} — workflow-scoped ({@code nodeId == ""}),
 *       numeric summary + recent per-sweep counts (VOLUME).
 *   <li>{@code OUTPUT_SCHEMA}/{@code BEHAVIOR} — structural aggregate stored in
 *       {@code signature} (field presence/type/null-rate; node-path run rates).
 * </ul>
 *
 * <p>{@code sampleCount < minSampleCount} (ReliabilityProperties) means the baseline is
 * not yet trustworthy; the detector suppresses anomalies for it (warm-up), never
 * fabricating a range from a tiny sample.
 */
@Entity
@Table(name = "metric_baselines")
public class MetricBaseline {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Column(name = "node_id", nullable = false, updatable = false)
    private String nodeId;

    @Column(name = "metric", nullable = false, updatable = false, length = 32)
    private String metric;

    @Column(name = "mean")
    private Double mean;

    @Column(name = "median")
    private Double median;

    @Column(name = "stddev")
    private Double stddev;

    @Column(name = "p50")
    private Double p50;

    @Column(name = "p95")
    private Double p95;

    @Column(name = "p99")
    private Double p99;

    @Column(name = "min")
    private Double min;

    @Column(name = "max")
    private Double max;

    @Column(name = "mad")
    private Double mad;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signature")
    private Map<String, Object> signature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MetricBaseline() {
        // JPA
    }

    public static MetricBaseline create(
            UUID organizationId, UUID workflowId, String nodeId, String metric) {
        MetricBaseline b = new MetricBaseline();
        b.id = UUID.randomUUID();
        b.organizationId = organizationId;
        b.workflowId = workflowId;
        b.nodeId = nodeId == null ? "" : nodeId;
        b.metric = metric;
        Instant now = now();
        b.createdAt = now;
        b.updatedAt = now;
        return b;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private void touch() {
        this.updatedAt = now();
    }

    /** Replaces the numeric summary with a fresh robust-stats sample. */
    public void setNumeric(RobustStats.Summary summary) {
        if (summary == null) {
            touch();
            return;
        }
        this.mean = summary.mean();
        this.median = summary.median();
        this.stddev = summary.stddev();
        this.mad = summary.mad();
        this.p50 = summary.p50();
        this.p95 = summary.p95();
        this.p99 = summary.p99();
        this.min = summary.min();
        this.max = summary.max();
        this.sampleCount = summary.n();
        touch();
    }

    /** Empties the numeric fields (e.g. a baseline whose samples fell below minimum). */
    public void clearNumeric() {
        this.mean = null;
        this.median = null;
        this.stddev = null;
        this.mad = null;
        this.p50 = null;
        this.p95 = null;
        this.p99 = null;
        this.min = null;
        this.max = null;
        this.mad = null;
        touch();
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
        touch();
    }

    public void setWindow(Instant start, Instant end) {
        this.windowStart = start;
        this.windowEnd = end;
        touch();
    }

    public void setSignature(Map<String, Object> signature) {
        this.signature = signature;
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

    public String getMetric() {
        return metric;
    }

    public Double getMean() {
        return mean;
    }

    public Double getMedian() {
        return median;
    }

    public Double getStddev() {
        return stddev;
    }

    public Double getP50() {
        return p50;
    }

    public Double getP95() {
        return p95;
    }

    public Double getP99() {
        return p99;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getMad() {
        return mad;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public Map<String, Object> getSignature() {
        return signature;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}