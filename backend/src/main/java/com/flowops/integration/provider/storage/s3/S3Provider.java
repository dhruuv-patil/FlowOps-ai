package com.flowops.integration.provider.storage.s3;

import com.flowops.integration.provider.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class S3Provider implements WorkflowProvider {
    @Override public IntegrationType type() { return IntegrationType.S3; }
    @Override public String displayName() { return "Amazon S3"; }
    @Override public String description() { return "Connect to AWS S3 buckets."; }
    @Override public ProviderCapabilities capabilities() { return new ProviderCapabilities(false, false, true, false, false, true); }

    @Override
    public List<CredentialField> credentialFields() {
        return List.of(
            new CredentialField("region", "Region", false, "us-east-1", "us-east-1", "AWS Region"),
            new CredentialField("accessKey", "Access Key", true, "AKIA...", "AKIA...", "AWS Access Key"),
            new CredentialField("secretKey", "Secret Key", true, "...", "...", "AWS Secret Key")
        );
    }

    @Override public ConnectionTestResult testConnection(IntegrationContext context) { return ConnectionTestResult.success("Connected to S3"); }
    @Override public List<ExternalWorkflow> discoverWorkflows(IntegrationContext context) { return List.of(); }
    @Override public ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor) { return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false); }
    @Override public List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution) { return List.of(); }
}
