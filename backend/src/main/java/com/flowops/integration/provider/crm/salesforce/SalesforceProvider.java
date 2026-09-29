package com.flowops.integration.provider.crm.salesforce;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SalesforceProvider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.SALESFORCE; }
    @Override public String displayName() { return "Salesforce"; }
    @Override public String description() { return "Connect to Salesforce CRM."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(true, false, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("clientId", "Client ID", true, "...", "...", "Salesforce Client ID"),
            new CredentialField("clientSecret", "Client Secret", true, "...", "...", "Salesforce Client Secret"),
            new CredentialField("instanceUrl", "Instance URL", false, "https://my-domain.my.salesforce.com", "...", "Instance URL")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        return ConnectionTestResult.success("Connected to Salesforce");
    }

    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
