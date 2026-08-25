package com.flowops.workflow;

import jakarta.validation.constraints.Size;

/** Body for publishing. The note is an optional human-readable changelog entry. */
public record PublishWorkflowRequest(@Size(max = 500) String note) {

    public PublishWorkflowRequest {
        note = note == null || note.isBlank() ? null : note.strip();
    }
}
