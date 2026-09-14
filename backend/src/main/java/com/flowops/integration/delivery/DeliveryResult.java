package com.flowops.integration.delivery;

/**
 * Result of a delivery attempt. Structured and honest: carries a provider-scoped
 * error {@code code}, a human {@code message}, and a {@code retryable} flag so the
 * execution engine can apply backoff for transient failures (timeout, rate limit,
 * 5xx) and report useful errors for permanent ones (auth failure, invalid config).
 * Secret material never appears in {@code errorMessage}.
 */
public record DeliveryResult(
        boolean success,
        Integer httpStatus,
        String code,
        String message,
        boolean retryable,
        long durationMs) {

    public static DeliveryResult success(Integer httpStatus, long durationMs) {
        return new DeliveryResult(true, httpStatus, null, null, false, durationMs);
    }

    public static DeliveryResult failure(
            String code, String message, boolean retryable, Integer httpStatus, long durationMs) {
        return new DeliveryResult(false, httpStatus, code, message, retryable, durationMs);
    }

    /** Structured error codes shared across delivery providers. */
    public static final class Code {
        public static final String AUTH_FAILED = "AUTH_FAILED";
        public static final String INVALID_CONFIG = "INVALID_CONFIG";
        public static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
        public static final String NETWORK_ERROR = "NETWORK_ERROR";
        public static final String RATE_LIMITED = "RATE_LIMITED";
        public static final String SEND_FAILED = "SEND_FAILED";
        public static final String SMTP_AUTH_FAILED = "SMTP_AUTH_FAILED";
        public static final String SMTP_CONNECTION_FAILED = "SMTP_CONNECTION_FAILED";

        private Code() {
        }
    }
}