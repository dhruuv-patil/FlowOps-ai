package com.flowops.api;

import com.flowops.workflow.nodes.NodeDefinition;
import com.flowops.workflow.validation.ValidationResult;
import java.util.List;

/** Object envelopes for the workflow collection endpoints (never bare arrays). */
public final class WorkflowEnvelopes {

    private WorkflowEnvelopes() {
    }

    public record Workflows(List<WorkflowSummaryResponse> workflows) {
    }

    public record Versions(List<WorkflowVersionResponse> versions) {
    }

    public record NodeTypes(List<NodeDefinition> nodeTypes) {
    }

    /**
     * Outcome of a publish attempt. Always HTTP 200: whether the graph could be
     * published is a business result, not a protocol error. {@code published} is
     * false with a populated {@code validation} when there were errors; true with
     * a {@code version} when the immutable snapshot was created.
     */
    public record PublishResult(
            boolean published, WorkflowVersionResponse version, ValidationResult validation) {

        public static PublishResult rejected(ValidationResult validation) {
            return new PublishResult(false, null, validation);
        }

        public static PublishResult ok(WorkflowVersionResponse version, ValidationResult validation) {
            return new PublishResult(true, version, validation);
        }
    }
}
