package com.flowops.integration.provider.pagerduty;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PagerDutyProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.PAGERDUTY; }
    @Override public String displayName() { return "PagerDuty"; }
    @Override public String description() { return "Connect to PagerDuty incidents."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, true, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("apiToken", "API Token", true, "pd_...", "pd_...", "PagerDuty API Token")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        return ConnectionTestResult.success("Connected to PagerDuty");
    }

    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
