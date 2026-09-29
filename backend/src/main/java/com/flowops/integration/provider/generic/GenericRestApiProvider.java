package com.flowops.integration.provider.generic;

import com.flowops.common.security.SsrfGuard;
import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.CredentialField;
import com.flowops.integration.provider.ExecutionPage;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExternalWorkflow;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderCapabilities;
import com.flowops.integration.provider.SyncCursor;
import com.flowops.integration.provider.WorkflowProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Universal / Generic REST API Provider.
 * Allows connecting to any custom internal or external HTTP/REST endpoint.
 */
@Component
public class GenericRestApiProvider implements WorkflowProvider {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public IntegrationType type() {
        return IntegrationType.REST_API;
    }

    @Override
    public String displayName() {
        return "Generic REST API";
    }

    @Override
    public String description() {
        return "Connect to any custom REST API endpoint using an API Key, Bearer Token, or Basic Auth.";
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(false, false, false, false, true, false);
    }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
                new CredentialField("baseUrl", "Base URL", false, "https://api.example.com", "https://api.example.com/v1", "The base URL for all API requests."),
                new CredentialField("authHeaderName", "Auth Header Name", false, "Authorization", "Authorization", "Header name used to deliver the credential."),
                new CredentialField("apiToken", "API Secret / Token", true, "your_api_token_here", "token_123456", "The raw API secret, token, or key."),
                new CredentialField("testEndpoint", "Health/Test Endpoint", false, "/health or /me", "/health", "Optional path appended to Base URL to test connection.")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        String baseUrl = context.credentials().get("baseUrl");
        String testEndpoint = context.credentials().get("testEndpoint");
        String headerName = context.credentials().get("authHeaderName");
        String token = context.credentials().get("apiToken");

        if (baseUrl == null || baseUrl.isBlank()) {
            return ConnectionTestResult.failure("Base URL is required");
        }

        String fullUrl = baseUrl.strip();
        if (testEndpoint != null && !testEndpoint.isBlank()) {
            if (!fullUrl.endsWith("/") && !testEndpoint.startsWith("/")) {
                fullUrl += "/";
            }
            fullUrl += testEndpoint.strip();
        }

        try {
            URI uri = SsrfGuard.validate(fullUrl);
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).GET().timeout(Duration.ofSeconds(10));

            if (headerName != null && !headerName.isBlank() && token != null && !token.isBlank()) {
                String val = token.trim();
                if (headerName.equalsIgnoreCase("Authorization") && !val.toLowerCase().startsWith("bearer ") && !val.toLowerCase().startsWith("basic ")) {
                    val = "Bearer " + val;
                }
                builder.header(headerName.trim(), val);
            }

            HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
                return ConnectionTestResult.success("Connected successfully to " + uri.getHost() + " (HTTP " + resp.statusCode() + ")");
            } else {
                return ConnectionTestResult.failure("Endpoint returned HTTP " + resp.statusCode());
            }
        } catch (com.flowops.common.error.ApiException ae) {
            return ConnectionTestResult.failure("Target rejected: " + ae.getMessage());
        } catch (Exception e) {
            return ConnectionTestResult.failure("Generic API connection error: " + e.getMessage());
        }
    }

    @Override
    public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) {
        return List.of();
    }

    @Override
    public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) {
        return ExecutionPage.empty();
    }

    @Override
    public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) {
        return List.of();
    }
}
