package com.flowops.integration.provider.linear;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LinearProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.LINEAR; }
    @Override public String displayName() { return "Linear"; }
    @Override public String description() { return "Connect to Linear issues and projects."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, true, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(new CredentialField("apiKey", "API Key", true, "lin_api_...", "lin_api_...", "Linear API Key"));
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        // Placeholder implement connection test check here later
        return ConnectionTestResult.success("Connected to Linear");
    }

    @Override
    public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }

    @Override
    public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) {
        return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false);
    }

    @Override
    public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) {
        return List.of();
    }
}
