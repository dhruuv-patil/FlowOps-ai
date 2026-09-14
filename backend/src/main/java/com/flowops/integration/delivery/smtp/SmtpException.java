package com.flowops.integration.delivery.smtp;

/**
 * A typed SMTP failure. {@code retryable} distinguishes transient transport
 * failures (timeout, connection refused, transient 4xx) from permanent ones
 * (authentication rejected, unknown recipient). The message never includes the
 * password.
 */
public final class SmtpException extends Exception {

    private final String code;
    private final boolean retryable;

    public SmtpException(String code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public SmtpException(String code, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }

    public static final class Codes {
        public static final String CONFIG = "INVALID_CONFIG";
        public static final String AUTH = "SMTP_AUTH_FAILED";
        public static final String CONNECTION = "SMTP_CONNECTION_FAILED";
        public static final String TIMEOUT = "CONNECTION_TIMEOUT";
        public static final String SEND = "SEND_FAILED";

        private Codes() {
        }
    }
}