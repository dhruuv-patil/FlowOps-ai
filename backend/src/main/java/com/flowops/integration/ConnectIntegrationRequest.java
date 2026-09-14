package com.flowops.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for connecting an integration. {@code type} selects the provider (only
 * {@code slack} is wired in M5) and {@code webhookUrl} is the secret being stored —
 * a Slack incoming-webhook URL. The URL is encrypted at rest and never returned to
 * the client; only its masked last-4 hint is ever shown back.
 */
public record ConnectIntegrationRequest(
        @NotBlank @Size(max = 40) String type,
        @NotBlank @Size(max = 500) String webhookUrl) {

    public ConnectIntegrationRequest {
        type = type == null ? null : type.strip().toLowerCase();
        webhookUrl = webhookUrl == null ? null : webhookUrl.strip();
    }
}
