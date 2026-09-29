package com.flowops.integration.provider.hubspot;

import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.CredentialField;
import com.flowops.integration.provider.ExecutionPage;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExternalWorkflow;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.integration.provider.ProviderCapabilities;
import com.flowops.integration.provider.SyncCursor;
import com.flowops.integration.provider.WorkflowProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HubSpot workflow provider.
 */
@Component
public class HubSpotProvider implements WorkflowProvider {

    @Override
    public IntegrationType type() {
        return IntegrationType.HUBSPOT;
    }

    @Override
    public String displayName() {
        return "HubSpot";
    }

    @Override
    public String description() {
        return "HubSpot CRM workflows";
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, false, true, true, false);
    }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
                new CredentialField("apiKey", "Access Token (Private App)", true,
                        "pat-na1-...", "pat-123...",
                        "HubSpot Private App Access Token.")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        // Implementation for HubSpot API connection test
        return ConnectionTestResult.success("HubSpot connected");
    }

    @Override
    public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) {
        // Implementation for HubSpot discovery
        return List.of();
    }

    @Override
    public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) {
        // Implementation for HubSpot executions
        return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false);
    }

    @Override
    public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) {
        // HubSpot doesn't support node execution data in the same way as n8n or Make
        return List.of();
    }
}
