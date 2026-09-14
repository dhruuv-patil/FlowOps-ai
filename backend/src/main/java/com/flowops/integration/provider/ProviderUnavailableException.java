package com.flowops.integration.provider;

/**
 * Provider temporarily unavailable (HTTP 5xx, timeout, network error).
 * The integration stays CONNECTED; sync is marked FAILED and retried
 * on the next scheduler cycle with bounded backoff.
 */
public class ProviderUnavailableException extends ProviderException {

    public ProviderUnavailableException(IntegrationType providerType, String message) {
        super(providerType, message);
    }

    public ProviderUnavailableException(IntegrationType providerType, String message, Throwable cause) {
        super(providerType, message, cause);
    }
}