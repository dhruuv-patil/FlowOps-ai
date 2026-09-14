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
 * A reusable AI agent configuration owned by exactly one organization.
 *
 * <p>Holds the system {@code instructions}, an optional {@code model} override, and
 * the allowlisted {@code tools} the AI service may invoke on this agent's behalf.
 * The provider API key is never stored here — it lives only on the AI service.
 * Foreign keys are plain UUIDs (contract §2 rule 6) so every query stays explicitly
 * org-scoped.
 */
@Entity
@Table(name = "ai_agents")
public class AiAgent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "instructions", nullable = false)
    private String instructions;

    @Column(name = "model", length = 80)
    private String model;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tools", nullable = false)
    private JsonNode tools;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiAgent() {
        // JPA
    }

    public static AiAgent create(
            UUID organizationId,
            UUID createdBy,
            String name,
            String description,
            String instructions,
            String model,
            JsonNode tools) {
        AiAgent agent = new AiAgent();
        agent.id = UUID.randomUUID();
        agent.organizationId = organizationId;
        agent.createdBy = createdBy;
        agent.name = name;
        agent.description = description;
        agent.instructions = instructions;
        agent.model = model;
        agent.tools = tools;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        agent.createdAt = now;
        agent.updatedAt = now;
        return agent;
    }

    private void touch() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** Updates the mutable fields. Callers pass the already-normalized values. */
    public void editDetails(
            String name, String description, String instructions, String model, JsonNode tools) {
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.model = model;
        this.tools = tools;
        touch();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getModel() {
        return model;
    }

    public JsonNode getTools() {
        return tools;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
