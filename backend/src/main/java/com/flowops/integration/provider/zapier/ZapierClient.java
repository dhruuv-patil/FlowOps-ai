package com.flowops.integration.provider.zapier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.integration.provider.DecryptedCredentials;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderAuthenticationException;
import com.flowops.integration.provider.ProviderConfigurationException;
import com.flowops.integration.provider.ProviderConnectionException;
import com.flowops.integration.provider.ProviderUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HTTP client for Zapier's webhook-based integration.
 *
 * <p><strong>Important:</strong> Zapier's public API does not expose run history or
 * workflow discovery — there is no REST endpoint to list Zaps or fetch their
 * execution runs. The only practical integration path for an MVP is:
 * <ul>
 *   <li>Connect by validating a webhook URL the user provides (they create a "Catch
 *       Hook" Zap and paste the unique URL here).</li>
 *   <li>FlowOps can then <em>send</em> events to that webhook (already covered by
 *       the outbound webhook provider + event dispatcher).</li>
 * </ul>
 * This client therefore implements only a lightweight connectivity test against the
 * provided webhook URL. The provider's capabilities will honestly report that
 * workflow discovery and execution history are unsupported.
 */
@Component
public class ZapierClient {

    private static final Logger log = LoggerFactory.getLogger(ZapierClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ZapierClient(ObjectMapper mapper) {
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public JsonNode testWebhook(IntegrationContext context) {
        String webhookUrl = webhookUrl(context);
        String body;
        try {
            body = mapper.writeValueAsString(
                    Map.of("event", "test", "source", "flowops"));
        } catch (IOException serializationFailed) {
            throw new ProviderConfigurationException(
                    IntegrationType.ZAPIER, "Could not serialize test payload");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return mapper.createObjectNode().put("status", "ok");
            }
            if (status == 401 || status == 403) {
                throw new ProviderAuthenticationException(
                        IntegrationType.ZAPIER, "Webhook rejected (HTTP " + status + ").");
            }
            if (status == 404) {
                throw new ProviderConfigurationException(
                        IntegrationType.ZAPIER, "Webhook URL not found (HTTP 404).");
            }
            throw new ProviderUnavailableException(
                    IntegrationType.ZAPIER, "Webhook error (HTTP " + status + ").");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderUnavailableException(
                    IntegrationType.ZAPIER, "Request interrupted", e);
        } catch (IOException e) {
            log.warn("Zapier webhook test failed: {}", e.getClass().getSimpleName());
            throw new ProviderConnectionException(
                    IntegrationType.ZAPIER, "Connection failed: " + e.getMessage(), e);
        }
    }

    private String webhookUrl(IntegrationContext context) {
        if (context == null || context.credentials() == null) {
            throw new ProviderConfigurationException(IntegrationType.ZAPIER, "Missing Zapier credentials");
        }
        String url = context.credentials().get("webhookUrl");
        if (url == null || url.isBlank()) {
            throw new ProviderConfigurationException(IntegrationType.ZAPIER, "Missing webhookUrl in credentials");
        }
        return url;
    }
}