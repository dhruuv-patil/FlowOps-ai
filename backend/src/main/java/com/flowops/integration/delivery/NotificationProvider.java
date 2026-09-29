package com.flowops.integration.delivery;

import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.CredentialField;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderCapabilities;
import java.util.List;

/**
 * SPI for delivery-channel providers (Slack, Discord, Email, Teams, webhook).
 *
 * <p>This is the notification-side analogue of {@code WorkflowProvider}: a FlowOps
 * <em>integration</em> is a connected channel, and a workflow's {@code notification}
 * node routes a message through the selected provider at run time. Providers never
 * hold credentials — they receive an in-memory {@link DeliveryContext} with the
 * decrypted credentials for one call and must never log, persist, or emit them.
 *
 * <p>Every operation is recorded by the integration telemetry layer (provider,
 * operation, duration, status, http status, retry count, error type), so call
 * latency behaves like any other reliability signal.
 */
public interface NotificationProvider extends com.flowops.integration.provider.IntegrationProvider {

    @Override
    default ProviderCapabilities capabilities() {
        // Delivery channels advertise webhook-style delivery.
        return new ProviderCapabilities(false, false, false, false, true, false);
    }


    /** Human-readable display name ("Slack", "Email (SMTP)", ...). */
    String displayName();

    /** Brief description for the provider catalog. */
    String description();

    /** Form schema the connect dialog renders. */
    List<CredentialField> credentialFields();

    /**
     * Which credential key carries the primary secret used for the masked last-4
     * {@code hint} shown in the UI (e.g. "webhookUrl", "password").
     */
    String secretKey();

    /**
     * Validates the stored credentials against the provider. Lightweight: for
     * HTTP webhooks this POSTs a test payload; for SMTP this opens + authenticates a
     * connection. Must not mutate state.
     */
    ConnectionTestResult testConnection(DeliveryContext context);

    /**
     * Delivers an already-rendered message. Honest and non-faking: returns a
     * {@link DeliveryResult} with a structured error code and a retryability flag so
     * the engine can apply backoff for transient failures and surface useful errors.
     */
    DeliveryResult deliver(DeliveryContext context, NotificationMessage message);

    /**
     * Sends a self-addressed test message (or, for SMTP, a test email to the
     * configured {@code fromAddress}). Used by the "Send test" action on a connected
     * integration card.
     */
    DeliveryResult sendTest(DeliveryContext context);
}