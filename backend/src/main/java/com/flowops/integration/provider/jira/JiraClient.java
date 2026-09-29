package com.flowops.integration.provider.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.security.SsrfGuard;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class JiraClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JiraClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    public HttpResponse<String> execute(String siteUrl, String email, String apiToken, String method, String path, String body) throws Exception {
        URI uri = SsrfGuard.validate(siteUrl + "/rest/api/3/" + path);
        String auth = Base64.getEncoder().encodeToString((email + ":" + apiToken).getBytes());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
