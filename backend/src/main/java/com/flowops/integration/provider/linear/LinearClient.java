package com.flowops.integration.provider.linear;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.security.SsrfGuard;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class LinearClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LinearClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    public HttpResponse<String> execute(String apiKey, String query, String variables) throws Exception {
        String url = "https://api.linear.app/graphql";
        URI uri = SsrfGuard.validate(url);

        String jsonPayload = objectMapper.writeValueAsString(java.util.Map.of("query", query, "variables", variables));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
