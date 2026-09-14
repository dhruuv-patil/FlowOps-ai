package com.flowops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the {@code flowops.webhook} tree.
 *
 * <p>{@code publicBaseUrl} is the origin an external caller reaches the backend at; it
 * is used only to compose the absolute inbound-webhook URL
 * ({@code {publicBaseUrl}/api/webhooks/{workflowId}/{token}}) handed back once at
 * generate time. It carries no secret and is safe to log.
 *
 * @param publicBaseUrl the externally-reachable backend origin, e.g.
 *                      {@code https://api.flowops.example.com}
 */
@ConfigurationProperties(prefix = "flowops.webhook")
public record WebhookProperties(@DefaultValue("http://localhost:8080") String publicBaseUrl) {

    public WebhookProperties {
        publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.strip();
        // Trim trailing slashes so publicBaseUrl + "/api/webhooks/..." never doubles the separator.
        while (publicBaseUrl.endsWith("/")) {
            publicBaseUrl = publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
        }
    }
}
