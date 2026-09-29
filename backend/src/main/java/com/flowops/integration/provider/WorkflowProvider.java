package com.flowops.integration.provider;

import java.util.List;

/**
 * SPI for external workflow automation providers (n8n, Make, Zapier, Temporal, etc.).
 *
 * <p>All methods receive an {@link IntegrationContext} containing the decrypted
 * credentials and the integration/organization identifiers. Implementations must
 * treat the credentials as sensitive: <strong>never log them, never persist them,
 * never pass them to telemetry or AI evidence</strong>.
 *
 * <p>Implementations should use JDK {@link java.net.http.HttpClient} with
 * connect and request timeouts (mirroring {@code AiServiceClient} and
 * {@code HttpRequestExecutor}), and must not leak provider-specific types
 * beyond this boundary — return the normalized {@link ExternalWorkflow},
 * {@link ExternalExecution}, and {@link ExternalNodeExecution} records.
 */
public interface WorkflowProvider extends IntegrationProvider {

    /** Stable lowercase identifier (e.g. "n8n", "make", "zapier"). */
    IntegrationType type();

    /** Human-readable display name. */
    String displayName();

    /** Brief description for the provider catalog. */
    String description();

    /** Static capability flags for UI and scheduler. */
    ProviderCapabilities capabilities();

    /**
     * Form schema the connect dialog renders for this provider. Each field's
     * {@code key} must match a credential key the provider's client reads from
     * {@link IntegrationContext#credentials()}. {@code secret} fields render as
     * password inputs and contribute to the masked last-4 hint.
     */
    List<CredentialField> credentialFields();

    /**
     * Tests the connection with the stored credentials.
     * Should make a lightweight authenticated call (e.g. list 1 workflow).
     *
     * @throws ProviderAuthenticationException if credentials are invalid (401/403)
     * @throws ProviderConfigurationException if base URL is wrong (404)
     * @throws ProviderUnavailableException if the provider is down (5xx/timeout)
     */
    ConnectionTestResult testConnection(IntegrationContext context);

    /**
     * Discovers all workflows available in the connected account.
     *
     * @throws ProviderAuthenticationException if credentials are invalid
     * @throws ProviderUnavailableException if the provider is down
     */
    List<ExternalWorkflow> discoverWorkflows(IntegrationContext context);

    /**
     * Fetches executions incrementally using a cursor.
     * The cursor is opaque to FlowOps; the provider defines its meaning.
     *
     * @param context the integration context with credentials
     * @param cursor the cursor from the last successful sync page (EMPTY for first page)
     * @return a page of executions and the next cursor
     */
    ExecutionPage<ExternalExecution> fetchExecutions(IntegrationContext context, SyncCursor cursor);

    /**
     * Fetches node/step execution data for a specific execution.
     *
     * @param context the integration context with credentials
     * @param execution the execution to fetch nodes for
     * @return list of node executions
     */
    List<ExternalNodeExecution> fetchNodeExecutions(IntegrationContext context, ExternalExecution execution);
}