package com.flowops.integration.delivery;

import com.flowops.integration.provider.DecryptedCredentials;
import java.util.Map;
import java.util.UUID;

/**
 * Context handed to a {@link NotificationProvider} for a single operation. Contains
 * the integration identity, non-secret metadata (e.g. the outbound webhook's event
 * type list), and the in-memory decrypted credentials. Credentials are never logged,
 * persisted, or sent to telemetry.
 */
public record DeliveryContext(
        UUID integrationId,
        UUID organizationId,
        Map<String, Object> metadata,
        DecryptedCredentials credentials) {
}