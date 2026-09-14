package com.flowops.integration.provider;

/**
 * Authentication/authorization failure (HTTP 401/403).
 * Indicates the stored credentials are invalid or expired.
 * Sync should transition the integration to AUTH_ERROR state.
 */
public class ProviderAuthenticationException extends ProviderException {

    public ProviderAuthenticationException(IntegrationType providerType, String message) {
        super(providerType, message);
    }

    public ProviderAuthenticationException(IntegrationType providerType, String message, Throwable cause) {
        super(providerType, message, cause);
    }
}