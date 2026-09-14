package com.flowops.integration.provider;

/**
 * Connection-level failure (DNS, TLS, refused, etc.) that is not
 * an authentication or rate-limit issue.
 */
public class ProviderConnectionException extends ProviderException {

    public ProviderConnectionException(IntegrationType providerType, String message) {
        super(providerType, message);
    }

    public ProviderConnectionException(IntegrationType providerType, String message, Throwable cause) {
        super(providerType, message, cause);
    }
}