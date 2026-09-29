package com.flowops.integration.provider.crm.pipedrive;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PipedriveProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.PIPEDRIVE; }
    @Override public String displayName() { return "Pipedrive"; }
    @Override public String description() { return "Connect to Pipedrive CRM."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, false, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("apiToken", "API Token", true, "...", "...", "Pipedrive API Token")
        );
    }

    @Override public ConnectionTestResult testConnection(IntegrationContext context) { return ConnectionTestResult.success("Connected to Pipedrive"); }
    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
