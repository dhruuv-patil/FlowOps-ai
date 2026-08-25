package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.domain.WorkflowVersion;
import java.time.Instant;
import java.util.UUID;

/** An immutable published version. {@code graph} is included only on the detail read. */
public record WorkflowVersionResponse(
        UUID id,
        int versionNumber,
        String note,
        JsonNode graph,
        UUID publishedBy,
        Instant createdAt) {

    public static WorkflowVersionResponse summary(WorkflowVersion version) {
        return new WorkflowVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getNote(),
                null,
                version.getPublishedBy(),
                version.getCreatedAt());
    }

    public static WorkflowVersionResponse full(WorkflowVersion version) {
        return new WorkflowVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getNote(),
                version.getGraph(),
                version.getPublishedBy(),
                version.getCreatedAt());
    }
}
