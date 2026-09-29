package com.flowops.integration.provider.vercel;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class VercelProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.VERCEL; }
    @Override public String displayName() { return "Vercel"; }
    @Override public String description() { return "Connect to Vercel deployments."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, true, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("apiToken", "API Token", true, "...", "...", "Vercel API Token")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        return ConnectionTestResult.success("Connected to Vercel");
    }

    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
