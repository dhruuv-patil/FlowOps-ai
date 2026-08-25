package com.flowops.domain;

/**
 * The state of one node within a run.
 *
 * <p>Every node starts {@code PENDING}. It becomes {@code SKIPPED} when no inbound
 * edge activates (the branch that would reach it was not taken), or runs through
 * {@code RUNNING} to {@code SUCCEEDED}/{@code FAILED}. {@code WAITING} is a node
 * (human approval) that has suspended the whole run.
 */
public enum NodeRunStatus {
    PENDING,
    RUNNING,
    WAITING,
    SUCCEEDED,
    FAILED,
    SKIPPED;

    /** Resolved nodes no longer block their successors from being scheduled. */
    public boolean isResolved() {
        return this == SUCCEEDED || this == FAILED || this == SKIPPED;
    }
}
