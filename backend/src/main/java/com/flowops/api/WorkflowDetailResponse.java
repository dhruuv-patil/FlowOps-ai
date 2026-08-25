package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.domain.Workflow;
import com.flowops.domain.WorkflowStatus;
import java.time.Instant;
import java.util.UUID;

/** Full workflow including the editable draft graph. Returned by get/create/save. */
public record WorkflowDetailResponse(
        UUID id,
        String name,
        String description,
        WorkflowStatus status,
        Integer latestVersion,
        JsonNode graph,
        Instant createdAt,
        Instant updatedAt) {

    public static WorkflowDetailResponse of(Workflow workflow) {
        return new WorkflowDetailResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                workflow.getLatestVersion(),
                workflow.getDraftGraph(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt());
    }
}
