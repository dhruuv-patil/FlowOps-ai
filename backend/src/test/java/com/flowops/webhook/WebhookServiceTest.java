package com.flowops.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.NullNode;
import com.flowops.api.WebhookEnvelopes;
import com.flowops.api.WebhookSecretResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.config.WebhookProperties;
import com.flowops.domain.Role;
import com.flowops.domain.Webhook;
import com.flowops.domain.Workflow;
import com.flowops.repository.WebhookRepository;
import com.flowops.repository.WorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Slice tests for {@link WebhookService}. Repositories are mocked; a real
 * {@link WebhookTokens} proves minting/verification happens end-to-end.
 *
 * <p>The invariants under test are the security-critical ones: a generated webhook
 * stores only the token's hash (never the token) plus a display hint; the full
 * tokened URL is returned exactly once and never re-derivable from a masked read; a
 * workflow owned by another tenant is indistinguishable from absent
 * ({@code WORKFLOW_NOT_FOUND}); and the public verify path returns the trusted org id
 * for a good token but an opaque {@code NOT_FOUND} for a missing/wrong/disabled one.
 */
class WebhookServiceTest {

    private static final String BASE_URL = "http://localhost:8080";

    private FlowOpsPrincipal admin(UUID orgId) {
        return new FlowOpsPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), orgId, Role.ADMIN, "admin@example.com");
    }

    private Workflow workflow(UUID orgId) {
        return Workflow.create(orgId, UUID.randomUUID(), "Wf", null, NullNode.getInstance());
    }

    private WebhookService service(WorkflowRepository workflows, WebhookRepository webhooks) {
        return new WebhookService(
                workflows,
                webhooks,
                new WebhookTokens(),
                new WebhookProperties(BASE_URL),
                mock(com.flowops.audit.AuditService.class));
    }

    @Test
    void generatePersistsTheHashNotTheTokenAndReturnsTheUrlOnce() {
        UUID orgId = UUID.randomUUID();
        Workflow wf = workflow(orgId);
        WorkflowRepository workflows = mock(WorkflowRepository.class);
        WebhookRepository webhooks = mock(WebhookRepository.class);
        when(workflows.findByIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.of(wf));
        when(webhooks.findByWorkflowIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.empty());

        WebhookSecretResponse response = service(workflows, webhooks).generate(admin(orgId), wf.getId());

        // The one-time URL embeds the full token and is rooted at the configured base.
        assertThat(response.url()).startsWith(BASE_URL + "/api/webhooks/" + wf.getId() + "/");
        String token = response.url().substring(response.url().lastIndexOf('/') + 1);
        assertThat(token).endsWith(response.tokenHint());

        // What gets persisted is the hash + hint — never the token itself.
        ArgumentCaptor<Webhook> saved = ArgumentCaptor.forClass(Webhook.class);
        verify(webhooks).save(saved.capture());
        Webhook stored = saved.getValue();
        assertThat(stored.getTokenHash()).matches("[0-9a-f]{64}");
        assertThat(stored.getTokenHash()).isNotEqualTo(token);
        assertThat(stored.getTokenHint()).isEqualTo(response.tokenHint());
        assertThat(stored.isEnabled()).isTrue();
    }

    @Test
    void getNeverExposesTheTokenAndMasksTheUrl() {
        UUID orgId = UUID.randomUUID();
        Workflow wf = workflow(orgId);
        WebhookTokens.Generated minted = new WebhookTokens().generate();
        Webhook webhook = Webhook.create(orgId, wf.getId(), UUID.randomUUID(), minted.hash(), minted.hint());

        WorkflowRepository workflows = mock(WorkflowRepository.class);
        WebhookRepository webhooks = mock(WebhookRepository.class);
        when(workflows.findByIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.of(wf));
        when(webhooks.findByWorkflowIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.of(webhook));

        WebhookEnvelopes.Webhook envelope = service(workflows, webhooks).get(admin(orgId), wf.getId());

        assertThat(envelope.webhook()).isNotNull();
        assertThat(envelope.webhook().tokenHint()).isEqualTo(minted.hint());
        assertThat(envelope.webhook().urlMasked()).contains("••••" + minted.hint());
        assertThat(envelope.webhook().urlMasked()).doesNotContain(minted.token());
    }

    @Test
    void getReturnsANullWebhookEnvelopeWhenNoneConfigured() {
        UUID orgId = UUID.randomUUID();
        Workflow wf = workflow(orgId);
        WorkflowRepository workflows = mock(WorkflowRepository.class);
        WebhookRepository webhooks = mock(WebhookRepository.class);
        when(workflows.findByIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.of(wf));
        when(webhooks.findByWorkflowIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.empty());

        assertThat(service(workflows, webhooks).get(admin(orgId), wf.getId()).webhook()).isNull();
    }

    @Test
    void crossTenantWorkflowIsNotFound() {
        UUID orgId = UUID.randomUUID();
        WorkflowRepository workflows = mock(WorkflowRepository.class);
        when(workflows.findByIdAndOrganizationId(any(), any())).thenReturn(Optional.empty());

        WebhookService service = service(workflows, mock(WebhookRepository.class));

        assertThatThrownBy(() -> service.generate(admin(orgId), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.WORKFLOW_NOT_FOUND);
    }

    @Test
    void verifyAndResolveReturnsTheTrustedOrgForAGoodToken() {
        UUID orgId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        WebhookTokens.Generated minted = new WebhookTokens().generate();
        Webhook webhook = Webhook.create(orgId, workflowId, UUID.randomUUID(), minted.hash(), minted.hint());
        WebhookRepository webhooks = mock(WebhookRepository.class);
        when(webhooks.findByWorkflowId(workflowId)).thenReturn(Optional.of(webhook));

        UUID resolved = service(mock(WorkflowRepository.class), webhooks)
                .verifyAndResolve(workflowId, minted.token());

        assertThat(resolved).isEqualTo(orgId);
    }

    @Test
    void verifyAndResolveIsOpaqueForMissingWrongOrDisabled() {
        UUID workflowId = UUID.randomUUID();
        WebhookTokens.Generated minted = new WebhookTokens().generate();

        // No row.
        WebhookRepository missing = mock(WebhookRepository.class);
        when(missing.findByWorkflowId(workflowId)).thenReturn(Optional.empty());
        assertOpaqueNotFound(() -> service(mock(WorkflowRepository.class), missing)
                .verifyAndResolve(workflowId, minted.token()));

        // Wrong token.
        Webhook enabled = Webhook.create(
                UUID.randomUUID(), workflowId, UUID.randomUUID(), minted.hash(), minted.hint());
        WebhookRepository wrong = mock(WebhookRepository.class);
        when(wrong.findByWorkflowId(workflowId)).thenReturn(Optional.of(enabled));
        assertOpaqueNotFound(() -> service(mock(WorkflowRepository.class), wrong)
                .verifyAndResolve(workflowId, "not-the-token"));

        // Disabled webhook, right token.
        Webhook disabled = Webhook.create(
                UUID.randomUUID(), workflowId, UUID.randomUUID(), minted.hash(), minted.hint());
        disabled.disable();
        WebhookRepository off = mock(WebhookRepository.class);
        when(off.findByWorkflowId(workflowId)).thenReturn(Optional.of(disabled));
        assertOpaqueNotFound(() -> service(mock(WorkflowRepository.class), off)
                .verifyAndResolve(workflowId, minted.token()));
    }

    private static void assertOpaqueNotFound(org.junit.jupiter.api.function.Executable call) {
        assertThatThrownBy(call::execute)
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void aFailedInboundAuthIsAuditedAnonymouslyAgainstTheTrustedOrgAndNeverCarriesTheToken() {
        UUID orgId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        WebhookTokens.Generated minted = new WebhookTokens().generate();
        Webhook webhook = Webhook.create(orgId, workflowId, UUID.randomUUID(), minted.hash(), minted.hint());
        WebhookRepository webhooks = mock(WebhookRepository.class);
        when(webhooks.findByWorkflowId(workflowId)).thenReturn(Optional.of(webhook));
        com.flowops.audit.AuditService audit = mock(com.flowops.audit.AuditService.class);

        WebhookService service = new WebhookService(
                mock(WorkflowRepository.class),
                webhooks,
                new WebhookTokens(),
                new WebhookProperties(BASE_URL),
                audit);

        assertOpaqueNotFound(() -> service.verifyAndResolve(workflowId, "not-the-token"));

        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);
        // The org is read from the stored row; the actor is null because the caller is
        // anonymous by definition on this path.
        verify(audit).record(
                org.mockito.ArgumentMatchers.eq(orgId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(com.flowops.domain.AuditAction.WEBHOOK_AUTH_FAILED),
                org.mockito.ArgumentMatchers.eq(workflowId.toString()),
                summary.capture());
        assertThat(summary.getValue()).doesNotContain("not-the-token").doesNotContain(minted.token());
    }

    @Test
    void generatingAWebhookAuditsTheHintNeverTheToken() {
        UUID orgId = UUID.randomUUID();
        Workflow wf = workflow(orgId);
        WorkflowRepository workflows = mock(WorkflowRepository.class);
        WebhookRepository webhooks = mock(WebhookRepository.class);
        when(workflows.findByIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.of(wf));
        when(webhooks.findByWorkflowIdAndOrganizationId(wf.getId(), orgId)).thenReturn(Optional.empty());
        com.flowops.audit.AuditService audit = mock(com.flowops.audit.AuditService.class);

        WebhookSecretResponse secret = new WebhookService(
                        workflows,
                        webhooks,
                        new WebhookTokens(),
                        new WebhookProperties(BASE_URL),
                        audit)
                .generate(admin(orgId), wf.getId());

        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);
        verify(audit).record(
                any(FlowOpsPrincipal.class),
                org.mockito.ArgumentMatchers.eq(com.flowops.domain.AuditAction.WEBHOOK_GENERATED),
                org.mockito.ArgumentMatchers.eq(wf.getId().toString()),
                summary.capture());
        String token = secret.url().substring(secret.url().lastIndexOf('/') + 1);
        assertThat(summary.getValue()).contains("••••" + secret.tokenHint()).doesNotContain(token);
    }
}
