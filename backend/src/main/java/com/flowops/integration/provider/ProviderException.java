package com.flowops.integration.provider;

/**
 * Base exception for provider-layer failures.
 */
public class ProviderException extends RuntimeException {

    private final IntegrationType providerType;

    public ProviderException(IntegrationType providerType, String message) {
        super(message);
        this.providerType = providerType;
    }

    public ProviderException(IntegrationType providerType, String message, Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
    }

    public IntegrationType getProviderType() {
        return providerType;
    }
}