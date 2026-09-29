package com.flowops.integration.provider.pm.asana;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AsanaProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.ASANA; }
    @Override public String displayName() { return "Asana"; }
    @Override public String description() { return "Connect to Asana tasks."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, false, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("apiToken", "API Token", true, "1/...", "...", "Asana Personal Access Token")
        );
    }

    @Override public ConnectionTestResult testConnection(IntegrationContext context) { return ConnectionTestResult.success("Connected to Asana"); }
    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
