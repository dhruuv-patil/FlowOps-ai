package com.flowops.domain;

/**
 * Lifecycle of a workflow.
 *
 * <p>{@code DRAFT} is editable and unpublished. {@code PUBLISHED} has at least one
 * immutable {@code workflow_versions} row and can be executed (M3).
 * {@code ARCHIVED} is hidden from the default listing but retained for audit.
 */
public enum WorkflowStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
}
