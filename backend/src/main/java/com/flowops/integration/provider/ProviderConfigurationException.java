package com.flowops.integration.provider;

/**
 * Provider is not configured, not registered, or misconfigured.
 */
public class ProviderConfigurationException extends ProviderException {

    public ProviderConfigurationException(IntegrationType providerType, String message) {
        super(providerType, message);
    }
}