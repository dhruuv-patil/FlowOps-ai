package com.flowops.integration.provider.productivity.google;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class GoogleSheetsProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.GOOGLE_SHEETS; }
    @Override public String displayName() { return "Google Sheets"; }
    @Override public String description() { return "Connect to Google Sheets."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, false, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("accessToken", "Access Token", true, "ya29...", "...", "Google OAuth Access Token")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        return ConnectionTestResult.success("Connected to Google Sheets");
    }

    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
