package com.flowops.integration.delivery;

import com.flowops.common.crypto.CredentialCipher;
import com.flowops.domain.Integration;
import com.flowops.integration.delivery.channels.WebhookProvider;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.repository.IntegrationCredentialRepository;
import com.flowops.repository.IntegrationRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delivers FlowOps lifecycle events to every outbound webhook the org has configured
 * to receive them. This is the event-sink half of the outbound webhook integration:
 * when an execution starts/completes/fails, an anomaly is detected, or a workflow is
 * published, each connected {@code webhook} integration whose {@code eventTypes}
 * filter matches is notified (HMAC-signed when a signing secret is configured).
 *
 * <p>Dispatch is intentionally fire-and-forget and exception-safe: a misbehaving
 * endpoint is logged (provider, event, HTTP status) and never breaks the run, the
 * anomaly record, or the publish. Credential material and the signing secret never
 * reach a log line.
 */
@Service
public class WebhookEventDispatcher {

    public static final class Event {
        public static final String EXECUTION_STARTED = "execution.started";
        public static final String EXECUTION_COMPLETED = "execution.completed";
        public static final String EXECUTION_FAILED = "execution.failed";
        public static final String ANOMALY_DETECTED = "anomaly.detected";
        public static final String WORKFLOW_PUBLISHED = "workflow.published";

        private Event() {
        }
    }

    private static final Logger log = LoggerFactory.getLogger(WebhookEventDispatcher.class);
    private static final String EVENT_TYPES_KEY = "eventTypes";

    private final IntegrationRepository integrations;
    private final IntegrationCredentialRepository credentials;
    private final CredentialCipher cipher;
    private final WebhookProvider webhookProvider;

    public WebhookEventDispatcher(
            IntegrationRepository integrations,
            IntegrationCredentialRepository credentials,
            CredentialCipher cipher,
            WebhookProvider webhookProvider) {
        this.integrations = integrations;
        this.credentials = credentials;
        this.cipher = cipher;
        this.webhookProvider = webhookProvider;
    }

    /**
     * Publishes {@code eventType} with {@code payload} to every connected webhook
     * integration in the org subscribed to that event type. One failing endpoint does
     * not stop the others, and dispatch never throws into the caller.
     */
    public void dispatch(UUID organizationId, String eventType, Map<String, Object> payload) {
        if (organizationId == null || eventType == null) {
            return;
        }
        List<Integration> targets = integrations
                .findByOrganizationIdAndStatus(organizationId, Integration.STATUS_CONNECTED)
                .stream()
                .filter(integration -> IntegrationType.WEBHOOK.wire()
                        .equalsIgnoreCase(integration.getType()))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        for (Integration integration : targets) {
            dispatchOne(organizationId, integration, eventType, payload);
        }
    }

    private void dispatchOne(
            UUID organizationId, Integration integration, String eventType, Map<String, Object> payload) {
        Map<String, String> config;
        try {
            config = credentials.findByIntegrationId(integration.getId())
                    .map(credential -> cipher.decryptToMap(credential.getCiphertext()))
                    .orElse(null);
        } catch (RuntimeException undecryptable) {
            log.warn("Webhook integration {} ({}) is connected but its credential "
                    + "could not be read; event {} skipped.",
                    integration.getId(), integration.getName(), eventType);
            return;
        }
        if (config == null) {
            return;
        }
        if (!subscribesTo(config, eventType)) {
            return;
        }
        DeliveryContext context = new DeliveryContext(
                integration.getId(), organizationId,
                integration.getMetadata() == null ? Map.of() : integration.getMetadata(),
                new com.flowops.integration.provider.DecryptedCredentials(config));
        try {
            DeliveryResult result = webhookProvider.dispatchEvent(context, eventType, payload);
            log.info("Webhook event {} → {} (HTTP {}){}", eventType, integration.getName(),
                    result.httpStatus() == null ? "n/a" : result.httpStatus(),
                    result.success() ? " success" : " failed: " + result.message());
        } catch (RuntimeException unexpected) {
            log.warn("Webhook event {} to {} failed unexpectedly: {}",
                    eventType, integration.getName(), unexpected.getClass().getSimpleName());
        }
    }

    /**
     * Whether the webhook's comma-separated {@code eventTypes} config includes the
     * event. An empty/unset filter defaults to receiving {@code execution.*} events so
     * a freshly connected webhook is useful immediately.
     */
    private static boolean subscribesTo(Map<String, String> config, String eventType) {
        String raw = config.get(EVENT_TYPES_KEY);
        if (raw == null || raw.isBlank()) {
            return eventType.startsWith("execution.");
        }
        Set<String> subscribed = Set.copyOf(
                java.util.Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toSet()));
        return subscribed.contains(eventType);
    }
}