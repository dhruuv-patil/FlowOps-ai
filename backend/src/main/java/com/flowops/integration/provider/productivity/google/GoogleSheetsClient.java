package com.flowops.integration.provider.productivity.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.security.SsrfGuard;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GoogleSheetsClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleSheetsClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    public HttpResponse<String> execute(String accessToken, String method, String path, String body) throws Exception {
        String url = "https://sheets.googleapis.com/v4/spreadsheets/" + path;
        URI uri = SsrfGuard.validate(url);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json");

        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
