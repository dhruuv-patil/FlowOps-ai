package com.flowops.execution;

import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.flowops.config.ExecutionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default {@link QueueService}: runs executions on a bounded local thread pool.
 * Active unless {@code flowops.execution.queue} is set to something other than
 * {@code memory} (e.g. a future {@code redis}), so the whole spine runs with zero
 * external infrastructure.
 *
 * <p>A submitted task drives one execution to a terminal or waiting state via the
 * engine. Any escaped throwable is turned into an honest run failure rather than a
 * silently swallowed exception, so a run never hangs in {@code RUNNING} forever.
 */
@Service
@ConditionalOnProperty(prefix = "flowops.execution", name = "queue", havingValue = "memory",
        matchIfMissing = true)
public class InMemoryQueueService implements QueueService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryQueueService.class);

    private final ExecutionEngine engine;
    private final ExecutorService workers;

    public InMemoryQueueService(ExecutionEngine engine, ExecutionProperties properties) {
        this.engine = engine;
        this.workers = Executors.newFixedThreadPool(properties.workerPoolSize(), namedDaemonFactory());
    }

    @Override
    public void enqueue(UUID executionId) {
        workers.submit(() -> {
            try {
                engine.run(executionId);
            } catch (Throwable failure) {
                // Last line of defence: never leave a run wedged in RUNNING.
                log.error("Execution {} crashed on the worker", executionId, failure);
                engine.abort(executionId, "The run stopped unexpectedly.");
            }
        });
    }

    @PreDestroy
    void shutdown() {
        workers.shutdown();
        try {
            if (!workers.awaitTermination(10, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ThreadFactory namedDaemonFactory() {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "flowops-exec-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
