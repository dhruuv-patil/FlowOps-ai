package com.flowops.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable snapshot of a workflow's graph, frozen at publish time.
 *
 * <p>Nothing here is ever updated after {@link #create}. A running execution (M3)
 * binds to one of these so it is unaffected by later edits to the draft.
 */
@Entity
@Table(name = "workflow_versions")
public class WorkflowVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "graph", nullable = false, updatable = false)
    private JsonNode graph;

    @Column(name = "note", length = 500, updatable = false)
    private String note;

    @Column(name = "published_by", nullable = false, updatable = false)
    private UUID publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkflowVersion() {
        // JPA
    }

    public static WorkflowVersion create(
            UUID workflowId, int versionNumber, JsonNode graph, String note, UUID publishedBy) {
        WorkflowVersion version = new WorkflowVersion();
        version.id = UUID.randomUUID();
        version.workflowId = workflowId;
        version.versionNumber = versionNumber;
        version.graph = graph;
        version.note = note;
        version.publishedBy = publishedBy;
        version.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public JsonNode getGraph() {
        return graph;
    }

    public String getNote() {
        return note;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
