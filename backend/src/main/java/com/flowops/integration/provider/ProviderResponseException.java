package com.flowops.integration.provider;

/**
 * Provider returned an error response (HTTP 4xx, excluding 401/403/429).
 * Indicates a configuration issue or malformed request.
 */
public class ProviderResponseException extends ProviderException {

    private final int statusCode;

    public ProviderResponseException(IntegrationType providerType, int statusCode, String message) {
        super(providerType, message);
        this.statusCode = statusCode;
    }

    public ProviderResponseException(IntegrationType providerType, int statusCode, String message, Throwable cause) {
        super(providerType, message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}