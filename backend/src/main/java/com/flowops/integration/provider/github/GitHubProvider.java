package com.flowops.integration.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.CredentialField;
import com.flowops.integration.provider.ExecutionPage;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExternalWorkflow;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderAuthenticationException;
import com.flowops.integration.provider.ProviderCapabilities;
import com.flowops.integration.provider.ProviderConfigurationException;
import com.flowops.integration.provider.ProviderUnavailableException;
import com.flowops.integration.provider.SyncCursor;
import com.flowops.integration.provider.WorkflowProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * GitHub Actions workflow provider adapter.
 * Uses GitHub REST API v3 with a PAT (classic or fine-grained).
 */
@Component
public class GitHubProvider implements WorkflowProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubProvider.class);
    private static final int DEFAULT_LIMIT = 50;

    private final GitHubClient client;

    public GitHubProvider(GitHubClient client) {
        this.client = client;
    }

    @Override
    public IntegrationType type() {
        return IntegrationType.GITHUB;
    }

    @Override
    public String displayName() {
        return "GitHub Actions";
    }

    @Override
    public String description() {
        return "GitHub Actions workflows and runs — discover workflows, sync execution "
                + "history with job/step telemetry for reliability baselines and anomaly detection.";
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                true,   // workflowDiscovery
                true,   // executionHistory
                true,   // nodeExecutionData (jobs + steps)
                true,   // incrementalSync (cursor via Link header)
                false,  // webhooks
                true    // tracing (steps have timing/sizes)
        );
    }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
                new CredentialField("owner", "Repository owner", false,
                        "acme-corp", "acme-corp",
                        "Owner of the GitHub repository (user or org)"),
                new CredentialField("repo", "Repository", false,
                        "platform", "platform",
                        "Repository to monitor for Actions workflows"),
                new CredentialField("pat", "Personal access token", true,
                        "github_pat_…", "github_pat_…",
                        "PAT with actions:read (classic or fine-grained). Stored encrypted."),
                new CredentialField("baseUrl", "API base URL", false,
                        "https://api.github.com", "https://api.github.com",
                        "GitHub Enterprise users override this with their instance base URL."));
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        String owner = owner(context);
        String repo = repo(context);
        try {
            client.listWorkflows(context, owner, repo, 1);
            return ConnectionTestResult.success("Connected to " + owner + "/" + repo);
        } catch (ProviderAuthenticationException e) {
            return ConnectionTestResult.failure("Authentication failed: " + e.getMessage());
        } catch (ProviderConfigurationException e) {
            return ConnectionTestResult.failure("Configuration error: " + e.getMessage());
        } catch (ProviderUnavailableException e) {
            return ConnectionTestResult.failure("Provider unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.warn("GitHub test connection failed", e);
            return ConnectionTestResult.failure("Connection test failed: " + e.getMessage());
        }
    }

    @Override
    public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) {
        String owner = owner(context);
        String repo = repo(context);
        JsonNode response = client.listWorkflows(context, owner, repo, 250);
        return GitHubWorkflowMapper.toExternal(response);
    }

    @Override
    public ExecutionPage<ExternalExecution> fetchExecutions(
            IntegrationContext context, SyncCursor cursor) {
        String owner = owner(context);
        String repo = repo(context);
        String after = cursor.isEmpty() ? null : cursor.value();
        JsonNode response = client.listRuns(context, owner, repo, DEFAULT_LIMIT, after);
        List<ExternalExecution> executions = GitHubExecutionMapper.toExternal(response);
        SyncCursor nextCursor = GitHubExecutionMapper.nextCursor(
                response.has("link") ? response.get("link").asText("") : "");
        boolean hasMore = !nextCursor.isEmpty();
        return new ExecutionPage<>(executions, nextCursor, hasMore);
    }

    @Override
    public List<ExternalNodeExecution> fetchNodeExecutions(
            IntegrationContext context, ExternalExecution execution) {
        String owner = owner(context);
        String repo = repo(context);
        long runId = Long.parseLong(execution.externalId());
        JsonNode response = client.listJobs(context, owner, repo, runId);
        return GitHubNodeExecutionMapper.toExternal(
                response, execution.externalId(), execution.workflowExternalId());
    }

    private String owner(IntegrationContext context) {
        if (context == null || context.credentials() == null) {
            throw new ProviderConfigurationException(
                    IntegrationType.GITHUB, "Missing GitHub credentials");
        }
        String owner = context.credentials().get("owner");
        if (owner == null || owner.isBlank()) {
            throw new ProviderConfigurationException(
                    IntegrationType.GITHUB, "Missing repository owner in credentials");
        }
        return owner;
    }

    private String repo(IntegrationContext context) {
        if (context == null || context.credentials() == null) {
            throw new ProviderConfigurationException(
                    IntegrationType.GITHUB, "Missing GitHub credentials");
        }
        String repo = context.credentials().get("repo");
        if (repo == null || repo.isBlank()) {
            throw new ProviderConfigurationException(
                    IntegrationType.GITHUB, "Missing repository name in credentials");
        }
        return repo;
    }
}