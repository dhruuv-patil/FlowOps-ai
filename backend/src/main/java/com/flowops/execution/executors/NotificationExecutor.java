package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import com.flowops.integration.delivery.DeliveryContext;
import com.flowops.integration.delivery.DeliveryResult;
import com.flowops.integration.delivery.NotificationMessage;
import com.flowops.integration.delivery.NotificationProvider;
import com.flowops.integration.delivery.NotificationProviderRegistry;
import com.flowops.integration.provider.CredentialField;
import com.flowops.integration.provider.DecryptedCredentials;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Notification: delivers a message through the selected channel's provider.
 *
 * <p>The node config declares {@code channel} (slack, email, discord, webhook, teams)
 * and, optionally, the {@code integrationId} to route through. The engine resolves the
 * connected integration's decrypted credentials at its tenant-scoped boundary and hands
 * them in on the non-persisted secret channel; this executor looks up the provider for
 * the channel, rebuilds those credentials by key, and delegates the real delivery to
 * {@link NotificationProvider#deliver}. It never holds credentials itself and never
 * fakes a delivery: when the channel has no provider or no connected integration it
 * succeeds with {@code delivered:false} and a clear reason so the run proceeds, and
 * logs a warning.
 *
 * <p>Any delivery failure is reported faithfully as a node failure (so the engine's
 * retry/backoff applies for retryable outcomes) with the provider's structured error
 * code — never the credential material.
 */
@Component
public class NotificationExecutor implements NodeExecutor {

    private final NotificationProviderRegistry providers;

    public NotificationExecutor(NotificationProviderRegistry providers) {
        this.providers = providers;
    }

    @Override
    public String type() {
        return "notification";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws Exception {
        String channel = ctx.configString("channel");
        String message = ctx.configString("message");
        if (channel == null || channel.isBlank()) {
            return NodeResult.fail("Notification has no channel configured.");
        }

        NotificationProvider provider;
        try {
            provider = providers.getByWire(channel);
        } catch (IllegalArgumentException unsupportedChannel) {
            return notDelivered(ctx, channel, message,
                    "No " + channel + " provider is supported.");
        }

        Map<String, String> credentials = collectCredentials(ctx, provider);
        if (credentials.isEmpty()) {
            return notDelivered(ctx, channel, message,
                    "No connected " + channel + " integration — configure an integration to deliver.");
        }

        // integrationId is node config (a non-secret reference), so it is safe for the
        // executor to read from config and attribute the DeliveryContext.
        java.util.UUID integrationId = parseIntegrationId(ctx);

        Map<String, Object> payload = new LinkedHashMap<>();
        String subject = ctx.configString("subject");
        if (subject != null && !subject.isBlank()) {
            payload.put("subject", subject);
        }
        String to = ctx.configString("to");
        if (to != null && !to.isBlank()) {
            payload.put("to", to);
        }

        NotificationMessage notification = new NotificationMessage(channel,
                message == null ? "" : message, payload);

        DeliveryResult result = provider.deliver(
                new DeliveryContext(integrationId, null, Map.of(), new DecryptedCredentials(credentials)),
                notification);

        ObjectNode output = ctx.mapper().createObjectNode();
        if (result.success()) {
            output.put("channel", channel);
            output.put("message", notification.text());
            output.put("delivered", true);
            if (result.httpStatus() != null) {
                output.put("status", result.httpStatus());
            }
            ctx.log().info("Notification delivered via " + channel
                    + (result.durationMs() > 0 ? " in " + result.durationMs() + "ms" : ""));
            return NodeResult.success(output);
        }

        ctx.log().warn("Notification via " + channel + " failed: " + result.message());
        output.put("channel", channel);
        output.put("message", notification.text());
        output.put("delivered", false);
        output.put("reason", result.message());
        // Non-2xx / transient failures fail the node so the engine's retry/backoff applies.
        return result.retryable()
                ? NodeResult.fail(result.message())
                : NodeResult.success(output);
    }

    /**
     * Reads each credential key the provider's connect form declares from the engine's
     * non-persisted secret channel. Empty when the engine resolved no secrets for the
     * referenced integration (i.e. it is not connected, is a different channel, or no
     * integrationId was given and there is no legacy Slack fallback).
     */
    private static Map<String, String> collectCredentials(
            NodeExecutionContext ctx, NotificationProvider provider) {
        Map<String, String> credentials = new LinkedHashMap<>();
        for (CredentialField field : provider.credentialFields()) {
            String value = ctx.secret(field.key());
            if (value != null && !value.isBlank()) {
                credentials.put(field.key(), value);
            }
        }
        return credentials;
    }

    private static java.util.UUID parseIntegrationId(NodeExecutionContext ctx) {
        String raw = ctx.configString("integrationId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(raw.strip());
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }

    private static NodeResult notDelivered(
            NodeExecutionContext ctx, String channel, String message, String reason) {
        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("channel", channel);
        output.put("message", message == null ? "" : message);
        output.put("delivered", false);
        output.put("reason", reason);
        ctx.log().warn("Notification not sent: " + reason + " Message was rendered only.");
        return NodeResult.success(output);
    }
}