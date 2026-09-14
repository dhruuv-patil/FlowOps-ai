package com.flowops.integration.delivery.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.integration.delivery.DeliveryResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Shared HTTP POST delivery for webhook-style providers (Slack, Discord, Teams, the
 * outbound webhook provider). Performs the send and classifies the outcome into a
 * structured {@link DeliveryResult} with a provider-scoped error code and a
 * retryable flag. Never logs the URL's query/secret, the request body, or headers.
 *
 * <p>Status classification (mirrors the n8n client's mapping but for delivery):
 * <ul>
 *   <li>2xx — success</li>
 *   <li>401/403 — {@code AUTH_FAILED}, not retryable</li>
 *   <li>408/504 — {@code CONNECTION_TIMEOUT}, retryable (aligns with Phase 9 example)</li>
 *   <li>429 — {@code RATE_LIMITED}, retryable</li>
 *   <li>5xx — {@code SEND_FAILED}, retryable</li>
 *   <li>other 4xx — {@code SEND_FAILED}, not retryable</li>
 * </ul>
 */
@Component
public class WebhookDeliverer {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;
    private final ObjectMapper mapper;

    public WebhookDeliverer(ObjectMapper mapper) {
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * POSTs {@code payload} as JSON to {@code url} with the given headers.
     *
     * @param requestTimeout per-call request timeout (null → 30s default)
     */
    public DeliveryResult postJson(
            String url,
            Map<String, String> headers,
            Map<String, Object> payload,
            Duration requestTimeout) {
        String body;
        try {
            body = mapper.writeValueAsString(payload);
        } catch (IOException serializationFailed) {
            return DeliveryResult.failure(
                    DeliveryResult.Code.SEND_FAILED,
                    "The message payload could not be serialized.",
                    false, null, 0);
        }
        return post(url, headers, body, requestTimeout);
    }

    /**
     * POSTs a raw string body to {@code url}. Used when the body is a ready-made JSON
     * string (or a test payload) rather than a builder-produced map.
     */
    public DeliveryResult post(
            String url,
            Map<String, String> headers,
            String body,
            Duration requestTimeout) {

        long start = System.nanoTime();
        Duration timeout = requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout;

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (headers != null) {
            headers.forEach(request::header);
        }
        request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        try {
            HttpResponse<String> response = client.send(
                    request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = millisSince(start);
            return classify(response.statusCode(), durationMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return DeliveryResult.failure(
                    DeliveryResult.Code.NETWORK_ERROR, "Delivery was interrupted.", true, null,
                    millisSince(start));
        } catch (IOException | IllegalArgumentException networkFailure) {
            return DeliveryResult.failure(
                    DeliveryResult.Code.NETWORK_ERROR,
                    "Could not reach the webhook endpoint.", true, null, millisSince(start));
        }
    }

    private DeliveryResult classify(int status, long durationMs) {
        if (status >= 200 && status < 300) {
            return DeliveryResult.success(status, durationMs);
        }
        if (status == 401 || status == 403) {
            return DeliveryResult.failure(
                    DeliveryResult.Code.AUTH_FAILED,
                    "The webhook rejected the request (HTTP " + status + ").",
                    false, status, durationMs);
        }
        if (status == 408 || status == 504) {
            return DeliveryResult.failure(
                    DeliveryResult.Code.CONNECTION_TIMEOUT,
                    "The endpoint did not respond within the configured timeout.",
                    true, status, durationMs);
        }
        if (status == 429) {
            return DeliveryResult.failure(
                    DeliveryResult.Code.RATE_LIMITED,
                    "Rate limited by the endpoint (HTTP 429).",
                    true, status, durationMs);
        }
        if (status >= 500) {
            return DeliveryResult.failure(
                    DeliveryResult.Code.SEND_FAILED,
                    "The endpoint reported a server error (HTTP " + status + ").",
                    true, status, durationMs);
        }
        return DeliveryResult.failure(
                DeliveryResult.Code.SEND_FAILED,
                "The endpoint rejected the request (HTTP " + status + ").",
                false, status, durationMs);
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}