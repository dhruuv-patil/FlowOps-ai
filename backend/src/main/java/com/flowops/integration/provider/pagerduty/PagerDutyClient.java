package com.flowops.integration.provider.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.security.SsrfGuard;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class PagerDutyClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PagerDutyClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    public HttpResponse<String> execute(String apiToken, String method, String path, String body) throws Exception {
        String url = "https://api.pagerduty.com/" + path;
        URI uri = SsrfGuard.validate(url);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Token token=" + apiToken)
                .header("Accept", "application/vnd.pagerduty+json;version=2")
                .header("Content-Type", "application/json");

        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
