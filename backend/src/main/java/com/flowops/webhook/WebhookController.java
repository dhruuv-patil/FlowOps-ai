package com.flowops.webhook;

import com.flowops.api.WebhookEnvelopes;
import com.flowops.api.WebhookSecretResponse;
import com.flowops.domain.Role;
import com.flowops.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound-webhook management for a workflow (M5). Authenticated and org-scoped — the
 * tenant is always the caller's organization (from the principal, never the path or
 * body). Distinct from the public ingress in {@link WebhookIngressController}: this
 * surface shows the owner the real state (enabled? configured?) and mints tokens.
 *
 * <p>Reading the masked state is open to any member; generating, toggling, and deleting
 * are administrative actions gated to {@link Role#ADMIN} and above — exactly like
 * connecting an integration. Generate returns the one-time URL-bearing response; every
 * other method returns masked state or {@code 204}.
 */
@RestController
@RequestMapping("/api/workflows/{workflowId}/webhook")
@Tag(name = "Webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    @Operation(summary = "Get the workflow's inbound webhook state (masked; never the token)")
    public WebhookEnvelopes.Webhook get(@PathVariable UUID workflowId) {
        return webhookService.get(AuthenticatedUser.require(), workflowId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate or rotate the webhook token; returns the URL once")
    public WebhookSecretResponse generate(@PathVariable UUID workflowId) {
        return webhookService.generate(AuthenticatedUser.requireRole(Role.ADMIN), workflowId);
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enable or disable the workflow's inbound webhook")
    public WebhookEnvelopes.Webhook setEnabled(
            @PathVariable UUID workflowId, @Valid @RequestBody SetWebhookEnabledRequest request) {
        return webhookService.setEnabled(
                AuthenticatedUser.requireRole(Role.ADMIN), workflowId, request.enabled());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the workflow's inbound webhook")
    public ResponseEntity<Void> delete(@PathVariable UUID workflowId) {
        webhookService.delete(AuthenticatedUser.requireRole(Role.ADMIN), workflowId);
        return ResponseEntity.noContent().build();
    }
}
