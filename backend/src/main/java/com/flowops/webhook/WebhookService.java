package com.flowops.webhook;

import com.flowops.api.WebhookEnvelopes;
import com.flowops.api.WebhookResponse;
import com.flowops.api.WebhookSecretResponse;
import com.flowops.audit.AuditService;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.config.WebhookProperties;
import com.flowops.domain.AuditAction;
import com.flowops.domain.Webhook;
import com.flowops.repository.WebhookRepository;
import com.flowops.repository.WorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbound-webhook management (org-scoped) plus the one un-authenticated verify path the
 * public ingress calls.
 *
 * <p>Every management method first resolves the workflow through
 * {@link #requireWorkflow} — {@code findByIdAndOrganizationId} — so a workflow owned by
 * another tenant is indistinguishable from one that does not exist
 * ({@link ErrorCode#WORKFLOW_NOT_FOUND}, contract §2 anti-enumeration). The token is
 * never returned by a management read: {@link #get} and {@link #toResponse} emit only a
 * masked hint. The full working URL is produced exactly once, by {@link #generate}.
 *
 * <p>Management actions are recorded in the audit trail, as is a failed inbound
 * authentication — see {@link #verifyAndResolve} for why only the branch carrying a
 * trusted organization id can be recorded.
 */
@Service
public class WebhookService {

    private final WorkflowRepository workflows;
    private final WebhookRepository webhooks;
    private final WebhookTokens tokens;
    private final WebhookProperties properties;
    private final AuditService audit;

    public WebhookService(
            WorkflowRepository workflows,
            WebhookRepository webhooks,
            WebhookTokens tokens,
            WebhookProperties properties,
            AuditService audit) {
        this.workflows = workflows;
        this.webhooks = webhooks;
        this.tokens = tokens;
        this.properties = properties;
        this.audit = audit;
    }

    /** Current masked state, or an envelope with a null webhook when none is configured. */
    @Transactional(readOnly = true)
    public WebhookEnvelopes.Webhook get(FlowOpsPrincipal principal, UUID workflowId) {
        requireWorkflow(principal, workflowId);
        return webhooks
                .findByWorkflowIdAndOrganizationId(workflowId, principal.organizationId())
                .map(webhook -> new WebhookEnvelopes.Webhook(toResponse(webhook)))
                .orElseGet(() -> new WebhookEnvelopes.Webhook(null));
    }

    /**
     * Mints a fresh token and upserts the workflow's webhook (rotating an existing one),
     * returning the full working URL <em>once</em>. Only the hash and a display hint are
     * stored; the URL cannot be recovered afterwards.
     */
    @Transactional
    public WebhookSecretResponse generate(FlowOpsPrincipal principal, UUID workflowId) {
        requireWorkflow(principal, workflowId);
        WebhookTokens.Generated minted = tokens.generate();

        Webhook webhook = webhooks
                .findByWorkflowIdAndOrganizationId(workflowId, principal.organizationId())
                .orElse(null);
        if (webhook == null) {
            webhook = Webhook.create(
                    principal.organizationId(), workflowId, principal.userId(), minted.hash(), minted.hint());
            webhooks.save(webhook);
        } else {
            webhook.rotate(minted.hash(), minted.hint());
        }
        // Only the masked hint reaches the trail — never `minted.token()`, which is the
        // credential itself and is shown to the caller exactly once, in the response.
        audit.record(
                principal,
                AuditAction.WEBHOOK_GENERATED,
                workflowId.toString(),
                "Generated a new inbound webhook token (••••" + minted.hint() + ").");
        return new WebhookSecretResponse(publicUrl(workflowId, minted.token()), minted.hint(), webhook.isEnabled());
    }
    @Transactional
    public WebhookEnvelopes.Webhook setEnabled(FlowOpsPrincipal principal, UUID workflowId, boolean enabled) {
        Webhook webhook = requireWebhook(principal, workflowId);
        if (enabled) {
            webhook.enable();
        } else {
            webhook.disable();
        }
        audit.record(
                principal,
                enabled ? AuditAction.WEBHOOK_ENABLED : AuditAction.WEBHOOK_DISABLED,
                workflowId.toString(),
                (enabled ? "Enabled" : "Disabled") + " the workflow's inbound webhook.");
        return new WebhookEnvelopes.Webhook(toResponse(webhook));
    }

    @Transactional
    public void delete(FlowOpsPrincipal principal, UUID workflowId) {
        Webhook webhook = requireWebhook(principal, workflowId);
        webhooks.delete(webhook);
        audit.record(
                principal,
                AuditAction.WEBHOOK_DELETED,
                workflowId.toString(),
                "Deleted the workflow's inbound webhook and destroyed its token.");
    }

    /**
     * The public ingress path: no principal exists. Loads the webhook by workflow id,
     * verifies the presented token in constant time, and returns the trusted
     * {@code organizationId} <em>read from the row</em>. Every failure — no row, wrong
     * token, disabled — throws the same opaque {@link ErrorCode#NOT_FOUND}; the miss path
     * still runs a constant-time compare against {@link WebhookTokens#DUMMY_HASH} so a
     * missing or disabled webhook is indistinguishable, by timing, from a wrong token.
     *
     * <p>A rejection is recorded as {@link AuditAction#WEBHOOK_AUTH_FAILED} with a null
     * actor — the request is anonymous by definition. It can only be recorded on the
     * branch where a webhook row exists, because that row is the sole trustworthy source
     * of the organization id; attributing an anonymous miss would mean inventing a tenant
     * from client input, which §2 forbids. The write is deliberately the last thing before
     * the throw, and runs in its own transaction ({@code REQUIRES_NEW}) so it survives
     * this {@code readOnly} context.
     */
    @Transactional(readOnly = true)
    public UUID verifyAndResolve(UUID workflowId, String token) {
        Webhook webhook = webhooks.findByWorkflowId(workflowId).orElse(null);
        if (webhook == null) {
            tokens.matches(token, WebhookTokens.DUMMY_HASH);
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        boolean ok = tokens.matches(token, webhook.getTokenHash());
        if (!ok || !webhook.isEnabled()) {
            audit.record(
                    webhook.getOrganizationId(),
                    null,
                    null,
                    AuditAction.WEBHOOK_AUTH_FAILED,
                    workflowId.toString(),
                    ok
                            ? "Rejected an inbound webhook call: the webhook is disabled."
                            : "Rejected an inbound webhook call: the token did not match.");
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return webhook.getOrganizationId();
    }

    /** Establishes the workflow exists in the caller's org (anti-enumeration choke point). */
    private void requireWorkflow(FlowOpsPrincipal principal, UUID workflowId) {
        workflows
                .findByIdAndOrganizationId(workflowId, principal.organizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.WORKFLOW_NOT_FOUND));
    }

    /** Resolves the workflow's webhook, org-scoped; absent → opaque {@code WORKFLOW_NOT_FOUND}. */
    private Webhook requireWebhook(FlowOpsPrincipal principal, UUID workflowId) {
        requireWorkflow(principal, workflowId);
        return webhooks
                .findByWorkflowIdAndOrganizationId(workflowId, principal.organizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.WORKFLOW_NOT_FOUND));
    }

    private WebhookResponse toResponse(Webhook webhook) {
        String hint = webhook.getTokenHint() == null ? "" : webhook.getTokenHint();
        String urlMasked = properties.publicBaseUrl()
                + "/api/webhooks/" + webhook.getWorkflowId() + "/••••" + hint;
        return new WebhookResponse(
                webhook.isEnabled(), webhook.getTokenHint(), urlMasked, webhook.getCreatedAt());
    }

    private String publicUrl(UUID workflowId, String token) {
        return properties.publicBaseUrl() + "/api/webhooks/" + workflowId + "/" + token;
    }
}
