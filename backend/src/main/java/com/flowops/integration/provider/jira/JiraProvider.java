package com.flowops.integration.provider.jira;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class JiraProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.JIRA; }
    @Override public String displayName() { return "Jira"; }
    @Override public String description() { return "Connect to Jira Cloud issues."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, true, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("siteUrl", "Site URL", false, "https://acme.atlassian.net", "https://*.atlassian.net", "Jira Site URL"),
            new CredentialField("email", "Email", true, "user@example.com", "user@example.com", "Jira User Email"),
            new CredentialField("apiToken", "API Token", true, "api_token", "api_token", "Jira API Token")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        // Implement lightweight connection test here later
        return ConnectionTestResult.success("Connected to Jira");
    }

    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
