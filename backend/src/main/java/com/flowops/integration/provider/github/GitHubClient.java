package com.flowops.integration.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.integration.provider.DecryptedCredentials;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderAuthenticationException;
import com.flowops.integration.provider.ProviderConfigurationException;
import com.flowops.integration.provider.ProviderConnectionException;
import com.flowops.integration.provider.ProviderRateLimitException;
import com.flowops.integration.provider.ProviderResponseException;
import com.flowops.integration.provider.ProviderUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HTTP client for GitHub REST API (Actions).
 *
 * <p>Authentication: a classic PAT (personal access token) with {@code workflow}
 * scope (read-only is enough for discovery + execution history) or a fine-grained
 * PAT with {@code Actions: Read} + {@code Metadata: Read}. The token is passed as
 * {@code Authorization: Bearer <token>}.</p>
 *
 * <p>Endpoints used (public REST v3):</p>
 * <ul>
 *   <li>{@code GET /repos/{owner}/{repo}/actions/workflows} — list workflows</li>
 *   <li>{@code GET /repos/{owner}/{repo}/actions/workflows/{id}/runs} — list runs with pagination</li>
 *   <li>{@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs} — job/step data per run</li>
 * </ul>
 *
 * <p>Pagination uses {@code Link} headers with {@code rel="next"}. The cursor is
 * the opaque {@code after} parameter for runs; for workflows it's page-based.</p>
 */
@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_API_BASE = "https://api.github.com";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public GitHubClient(ObjectMapper mapper) {
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public JsonNode listWorkflows(IntegrationContext context, String owner, String repo, int limit) {
        String path = "/repos/" + owner + "/" + repo + "/actions/workflows?per_page=" + Math.min(limit, 100);
        return get(context, path);
    }

    public JsonNode listRuns(IntegrationContext context, String owner, String repo, int limit, String afterCursor) {
        StringBuilder path = new StringBuilder(
                "/repos/" + owner + "/" + repo + "/actions/runs?per_page=" + Math.min(limit, 100));
        if (afterCursor != null && !afterCursor.isBlank()) {
            path.append("&after=").append(URLEncoder.encode(afterCursor, StandardCharsets.UTF_8));
        }
        return get(context, path.toString());
    }

    public JsonNode listJobs(IntegrationContext context, String owner, String repo, long runId) {
        String path = "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs?per_page=100";
        return get(context, path);
    }

    private JsonNode get(IntegrationContext context, String path) {
        String token = token(context);
        String base = baseUrl(context);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderUnavailableException(IntegrationType.GITHUB, "Request interrupted", e);
        } catch (IOException e) {
            log.warn("GitHub HTTP request failed: {}", e.getClass().getSimpleName());
            throw new ProviderConnectionException(
                    IntegrationType.GITHUB, "Connection failed: " + e.getMessage(), e);
        }
    }

    private JsonNode handleResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();
        if (status >= 200 && status < 300) {
            try {
                if (body == null || body.isBlank()) {
                    return mapper.createObjectNode();
                }
                return mapper.readTree(body);
            } catch (IOException e) {
                throw new ProviderResponseException(
                        IntegrationType.GITHUB, status, "Failed to parse response: " + e.getMessage(), e);
            }
        }
        if (status == 401 || status == 403) {
            throw new ProviderAuthenticationException(
                    IntegrationType.GITHUB, "Authentication failed (HTTP " + status + ").");
        }
        if (status == 404) {
            throw new ProviderConfigurationException(
                    IntegrationType.GITHUB, "Repository or workflow not found (HTTP 404).");
        }
        if (status == 422) {
            throw new ProviderConfigurationException(
                    IntegrationType.GITHUB, "Validation failed (HTTP 422): check PAT scopes.");
        }
        if (status == 429) {
            long retryAfter = 60;
            try {
                String retryHeader = response.headers().firstValue("Retry-After").orElse("60");
                retryAfter = Long.parseLong(retryHeader);
            } catch (NumberFormatException ignored) {
            }
            throw new ProviderRateLimitException(
                    IntegrationType.GITHUB, "Rate limited (HTTP 429).", retryAfter);
        }
        if (status >= 500) {
            throw new ProviderUnavailableException(
                    IntegrationType.GITHUB, "Provider error (HTTP " + status + ").");
        }
        throw new ProviderResponseException(
                IntegrationType.GITHUB, status, "Request failed (HTTP " + status + ").");
    }

    private String token(IntegrationContext context) {
        if (context == null || context.credentials() == null) {
            throw new ProviderAuthenticationException(IntegrationType.GITHUB, "Missing GitHub credentials");
        }
        String token = context.credentials().get("pat");
        if (token == null || token.isBlank()) {
            throw new ProviderAuthenticationException(IntegrationType.GITHUB, "Missing PAT in credentials");
        }
        return token;
    }

    private String baseUrl(IntegrationContext context) {
        if (context == null || context.credentials() == null) {
            return DEFAULT_API_BASE;
        }
        String base = context.credentials().get("baseUrl");
        return (base == null || base.isBlank()) ? DEFAULT_API_BASE : base.replaceAll("/+$", "");
    }
}