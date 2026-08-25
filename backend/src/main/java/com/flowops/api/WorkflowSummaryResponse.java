package com.flowops.api;

import com.flowops.domain.Workflow;
import com.flowops.domain.WorkflowStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Row shape for the workflows list. Omits the full graph — only a node count is
 * exposed — so the listing stays light.
 */
public record WorkflowSummaryResponse(
        UUID id,
        String name,
        String description,
        WorkflowStatus status,
        Integer latestVersion,
        int nodeCount,
        Instant createdAt,
        Instant updatedAt) {

    public static WorkflowSummaryResponse of(Workflow workflow, int nodeCount) {
        return new WorkflowSummaryResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                workflow.getLatestVersion(),
                nodeCount,
                workflow.getCreatedAt(),
                workflow.getUpdatedAt());
    }
}
