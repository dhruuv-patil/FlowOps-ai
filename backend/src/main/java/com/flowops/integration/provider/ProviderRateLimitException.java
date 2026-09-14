package com.flowops.integration.provider;

/**
 * Rate limit exceeded (HTTP 429).
 * The provider may include a retry-after header; the scheduler
 * should back off accordingly.
 */
public class ProviderRateLimitException extends ProviderException {

    private final long retryAfterSeconds;

    public ProviderRateLimitException(IntegrationType providerType, String message, long retryAfterSeconds) {
        super(providerType, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ProviderRateLimitException(IntegrationType providerType, String message, Throwable cause, long retryAfterSeconds) {
        super(providerType, message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}