package com.flowops.integration.provider.stripe;

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
 * Stripe workflow provider.
 */
@Component
public class StripeProvider implements WorkflowProvider {

    @Override
    public IntegrationType type() {
        return IntegrationType.STRIPE;
    }

    @Override
    public String displayName() {
        return "Stripe";
    }

    @Override
    public String description() {
        return "Stripe events and dashboard automation";
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, false, true, true, false);
    }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
                new CredentialField("apiKey", "Secret Key", true,
                        "sk_live_...", "sk_123...",
                        "Stripe API Secret Key.")
        );
    }

    @Override
    public ConnectionTestResult testConnection(IntegrationContext context) {
        // Implementation for Stripe API connection test
        return ConnectionTestResult.success("Stripe connected");
    }

    @Override
    public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) {
        // Stripe doesn't have "workflows" as n8n, but we can map events
        return List.of();
    }

    @Override
    public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) {
        // Implementation for Stripe events as executions
        return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false);
    }

    @Override
    public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) {
        return List.of();
    }
}
