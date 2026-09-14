package com.flowops.integration.provider;

import java.util.Map;
import java.util.UUID;

/**
 * Context passed to a {@link WorkflowProvider} for an operation.
 * Contains the minimal data needed to authenticate and scope the call.
 *
 * <p>{@code credentials} is the <strong>only</strong> object in the provider
 * layer that ever holds plaintext secrets. It is created in memory by the
 * sync service, used for the HTTP calls, and immediately discarded.
 * It is never logged, never persisted, and never enters telemetry or AI
 * evidence.
 */
public record IntegrationContext(
        UUID integrationId,
        UUID organizationId,
        DecryptedCredentials credentials
) {
}