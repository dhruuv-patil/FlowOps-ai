package com.flowops.execution;

import com.flowops.audit.AuditService;
import com.flowops.domain.AuditAction;
import com.flowops.repository.WorkflowExecutionRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup recovery for executions that were {@code QUEUED} or {@code RUNNING} when
 * the previous JVM exited.
 *
 * <p>The default {@link InMemoryQueueService} lives inside the worker pool: when
 * the process stops, every in-flight task is abandoned. Without this sweep the
 * next boot would find a database full of {@code RUNNING} rows that no worker
 * will ever look at again — the runs would be wedged forever and the user would
 * have no honest way to clear them.
 *
 * <h2>What it does</h2>
 * On {@link ApplicationStartedEvent} (after the context is fully refreshed and
 * Flyway has applied) the service:
 * <ol>
 *   <li>Loads the ids of every non-terminal execution, oldest first.</li>
 *   <li>For each one, resets any per-node {@code RUNNING} or {@code PENDING} rows
 *       back to {@code PENDING} so the engine re-attempts them; terminal node
 *       states ({@code SUCCEEDED}, {@code FAILED}, {@code SKIPPED},
 *       {@code WAITING}) are preserved — the engine is re-entrant and will reuse
 *       their outputs.</li>
 *   <li>Re-enqueues the execution on the same {@link QueueService} used by
 *       manual and webhook starts. The engine treats a re-entry the same as a
 *       resume: idempotent on terminal runs, and it re-uses everything that
 *       already succeeded.</li>
 *   <li>Records one anonymous audit event per re-enqueue, so the operator can
 *       see in the trail that a recovery happened.</li>
 * </ol>
 *
 * <p>{@code WAITING} runs (human approval) are deliberately not in the
 * recoverable set: a parked run is not a crash, and the user can still come
 * back and decide. A future change could re-emit a notification for them, but
 * that is out of scope for the MVP.
 *
 * <h2>Failure handling</h2>
 * The sweep is best-effort. A failure on one execution is logged and the loop
 * continues with the next; nothing throws out of the listener, so a buggy
 * recovery path can never block the application from coming up.
 */
@Service
public class ExecutionRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionRecoveryService.class);

    private final WorkflowExecutionRepository executions;
    private final ExecutionStore store;
    private final QueueService queue;
    private final AuditService audit;

    public ExecutionRecoveryService(
            WorkflowExecutionRepository executions,
            ExecutionStore store,
            QueueService queue,
            AuditService audit) {
        this.executions = executions;
        this.store = store;
        this.queue = queue;
        this.audit = audit;
    }

    /**
     * Runs once on application start. The listener swallows its own failures so
     * a transient error during recovery (e.g. DB hiccup) cannot prevent the
     * application from accepting traffic.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void recoverOnStartup() {
        try {
            recover();
        } catch (RuntimeException recoveryFailure) {
            // Never block startup on a recovery error. A supervisor / healthcheck
            // can still see the wedged rows and an operator can re-trigger the
            // sweep by restarting; the alternative (a wedged boot) is worse.
            log.error("Execution recovery sweep failed on startup; some runs may be wedged.",
                    recoveryFailure);
        }
    }

    /**
     * The actual sweep, exposed for tests. Returns the number of executions
     * re-enqueued.
     */
    @Transactional
    public int recover() {
        List<UUID> ids = executions.findRecoverableExecutionIds();
        if (ids.isEmpty()) {
            log.info("Startup recovery sweep: no runs to recover.");
            return 0;
        }
        log.info("Startup recovery sweep: re-enqueueing {} non-terminal run(s).", ids.size());
        int requeued = 0;
        for (UUID id : ids) {
            try {
                int reset = store.resetStuckNodesForRecovery(id);
                queue.enqueue(id);
                requeued++;
                log.info("Recovered execution {} ({} stuck node(s) reset to PENDING).", id, reset);
                // The org id comes from the trusted execution row, never from the
                // actor (there is no actor on a startup recovery). The audit
                // event is written in its own REQUIRES_NEW transaction inside
                // AuditService, so this @Transactional context can safely read
                // before it.
                UUID orgId = store.getExecution(id).getOrganizationId();
                audit.record(
                        orgId,
                        null,
                        null,
                        AuditAction.EXECUTION_RECOVERED,
                        id.toString(),
                        "Recovered execution " + id + " on startup: "
                                + reset + " stuck node(s) reset and the run re-queued.");
            } catch (RuntimeException perRunFailure) {
                // One bad run must not block the rest. The row stays wedged; the
                // next sweep (or an operator) can retry.
                log.warn("Failed to recover execution {}: {}", id, perRunFailure.getMessage());
            }
        }
        log.info("Startup recovery sweep complete: {}/{} run(s) re-enqueued.",
                requeued, ids.size());
        return requeued;
    }
}
