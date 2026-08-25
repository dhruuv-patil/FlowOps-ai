package com.flowops.execution;

import java.util.UUID;

/**
 * Hands a queued execution to a worker for running. The default
 * {@link InMemoryQueueService} runs it on a bounded local thread pool — no external
 * dependency. A Redis-backed implementation (multi-instance, durable) is an opt-in
 * drop-in behind this same interface, selected by configuration.
 */
public interface QueueService {

    /** Schedules {@code executionId} to run (or resume) on a worker thread. */
    void enqueue(UUID executionId);
}
