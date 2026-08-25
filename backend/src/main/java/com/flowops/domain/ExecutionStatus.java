package com.flowops.domain;

/**
 * The lifecycle of a single workflow run.
 *
 * <pre>
 *   QUEUED ─▶ RUNNING ─▶ SUCCEEDED
 *               │  ▲          FAILED
 *               ▼  │          CANCELED
 *            WAITING (human approval)
 * </pre>
 *
 * <p>{@code WAITING} is a suspended run: the engine has released its worker thread
 * and will resume when the pending decision arrives. The four terminal states
 * ({@code SUCCEEDED}, {@code FAILED}, {@code CANCELED}) never transition again.
 */
public enum ExecutionStatus {
    QUEUED,
    RUNNING,
    WAITING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }
}
