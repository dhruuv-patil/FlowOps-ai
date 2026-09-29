package com.flowops.integration.provider;

import java.util.Map;

/**
 * Plaintext credentials for a provider, used only during a single sync cycle.
 *
 * <p><strong>Security contract:</strong> This object is the only place in the
 * provider layer where plaintext secrets exist. It is:
 * <ul>
 *   <li>Created in memory by {@link com.flowops.integration.sync.IntegrationSyncService}</li>
 *   <li>Passed to the {@link WorkflowProvider} for HTTP calls</li>
 *   <li><strong>Never</strong> logged (the provider must not log it)</li>
 *   <li><strong>Never</strong> persisted</li>
 *   <li><strong>Never</strong> sent to telemetry, anomalies, or AI evidence</li>
 * </ul>
 * Implementations should treat this as sensitive and avoid accidental
 * serialization (e.g. in toString, exception messages, or structured logs).
 */
public record DecryptedCredentials(Map<String, String> secrets) {

    /**
     * Creates credentials from a map of key-value pairs.
     * Keys are provider-specific (e.g. "apiKey", "accessToken", "baseUrl").
     */
    public DecryptedCredentials {
        if (secrets == null) {
            throw new IllegalArgumentException("Credentials map cannot be null");
        }
        secrets = Map.copyOf(secrets);
    }

    @Override
    public String toString() {
        return "DecryptedCredentials[redacted]";
    }

    /** Returns the secret for a key, or null if not present. */
    public String get(String key) {
        return secrets.get(key);
    }

    /** Returns the secret for a key, throwing if absent. */
    public String require(String key) {
        String v = secrets.get(key);
        if (v == null) {
            throw new IllegalStateException("Required credential missing: " + key);
        }
        return v;
    }
}