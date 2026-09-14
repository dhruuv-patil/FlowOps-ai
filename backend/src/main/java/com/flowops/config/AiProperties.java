package com.flowops.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the {@code flowops.ai} tree: how the backend reaches the Python AI service.
 *
 * <p>The browser never talks to the AI service directly — the backend brokers every
 * call and presents {@code token} in the {@code X-Service-Token} header. The token is
 * a shared secret between the two services; it (and the provider key, which never
 * leaves the AI service at all) must never be logged.
 *
 * @param baseUrl        the AI service origin, e.g. {@code http://localhost:8100}
 * @param token          shared service-to-service secret; empty disables the header
 * @param connectTimeout TCP connect timeout for calls to the AI service
 * @param requestTimeout overall per-request timeout (agent runs may take several
 *                       provider round-trips, so this is generous)
 */
@ConfigurationProperties(prefix = "flowops.ai")
public record AiProperties(
        @DefaultValue("http://localhost:8100") String baseUrl,
        @DefaultValue("") String token,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("120s") Duration requestTimeout) {

    public AiProperties {
        baseUrl = baseUrl == null ? "" : baseUrl.strip();
        // Trim a trailing slash so baseUrl + "/ai/..." never doubles the separator.
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        token = token == null ? "" : token.strip();
    }
}
