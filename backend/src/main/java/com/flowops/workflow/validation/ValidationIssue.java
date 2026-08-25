package com.flowops.workflow.validation;

/**
 * One problem found in a graph. {@code nodeId}/{@code edgeId} are null when the
 * issue is about the graph as a whole (e.g. no trigger). {@code code} is stable
 * for the frontend to branch on; {@code message} is safe to show a user.
 */
public record ValidationIssue(Severity severity, String code, String message, String nodeId, String edgeId) {

    public static ValidationIssue graphError(String code, String message) {
        return new ValidationIssue(Severity.ERROR, code, message, null, null);
    }

    public static ValidationIssue nodeError(String code, String message, String nodeId) {
        return new ValidationIssue(Severity.ERROR, code, message, nodeId, null);
    }

    public static ValidationIssue nodeWarning(String code, String message, String nodeId) {
        return new ValidationIssue(Severity.WARNING, code, message, nodeId, null);
    }

    public static ValidationIssue edgeError(String code, String message, String edgeId) {
        return new ValidationIssue(Severity.ERROR, code, message, null, edgeId);
    }
}
