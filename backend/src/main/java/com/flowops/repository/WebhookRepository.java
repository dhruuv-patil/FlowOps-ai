package com.flowops.repository;

import com.flowops.domain.Webhook;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Webhook lookups. Management reads are org-scoped by construction
 * ({@link #findByWorkflowIdAndOrganizationId}); the single un-scoped lookup —
 * {@link #findByWorkflowId} — exists only for the public ingress, where no principal
 * is present. That path reads the trusted {@code organizationId} <em>from the row</em>
 * and threads it into the run; the client never supplies a tenant (contract §2).
 * There is no bare {@code findById} exposed to callers (mirrors {@code WorkflowRepository}).
 */
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    Optional<Webhook> findByWorkflowIdAndOrganizationId(UUID workflowId, UUID organizationId);

    /** Public ingress only — no principal exists; the org is then read from the row. */
    Optional<Webhook> findByWorkflowId(UUID workflowId);
}
