package com.flowops.integration.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.common.security.SsrfGuard;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderUnavailableException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    public JsonNode listWorkflows(IntegrationContext ctx, String owner, String repo, int limit) {
        String pat = ctx.credentials().require("pat");
        String baseUrl = ctx.credentials().get("baseUrl");
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.github.com";
        try {
            var response = execute(pat, baseUrl, "GET", "/repos/" + owner + "/" + repo + "/actions/workflows?per_page=" + limit, null);
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new ProviderUnavailableException(IntegrationType.GITHUB, "Failed to list workflows: " + e.getMessage());
        }
    }

    public JsonNode listRuns(IntegrationContext ctx, String owner, String repo, int limit, String after) {
        String pat = ctx.credentials().require("pat");
        String baseUrl = ctx.credentials().get("baseUrl");
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.github.com";
        String url = "/repos/" + owner + "/" + repo + "/actions/runs?per_page=" + limit;
        if (after != null) url += "&page=" + after;
        try {
            var response = execute(pat, baseUrl, "GET", url, null);
            ObjectNode root = (ObjectNode) objectMapper.readTree(response.body());
            root.put("link", response.headers().firstValue("Link").orElse(""));
            return root;
        } catch (Exception e) {
            throw new ProviderUnavailableException(IntegrationType.GITHUB, "Failed to list runs: " + e.getMessage());
        }
    }

    public JsonNode listJobs(IntegrationContext ctx, String owner, String repo, long runId) {
        String pat = ctx.credentials().require("pat");
        String baseUrl = ctx.credentials().get("baseUrl");
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.github.com";
        try {
            var response = execute(pat, baseUrl, "GET", "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs", null);
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new ProviderUnavailableException(IntegrationType.GITHUB, "Failed to list jobs: " + e.getMessage());
        }
    }

    public HttpResponse<String> execute(String pat, String baseUrl, String method, String path, String body) throws Exception {
        String url = baseUrl + path;
        URI uri = SsrfGuard.validate(url);
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + pat)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json");

        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
