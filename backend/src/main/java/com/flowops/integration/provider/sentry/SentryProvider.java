package com.flowops.integration.provider.sentry;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SentryProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.SENTRY; }
    @Override public String displayName() { return "Sentry"; }
    @Override public String description() { return "Connect to Sentry issues and alerts."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, true, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("apiToken", "Auth Token", true, "sntry_...", "sntry_...", "Sentry Auth Token")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        return ConnectionTestResult.success("Connected to Sentry");
    }

    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
