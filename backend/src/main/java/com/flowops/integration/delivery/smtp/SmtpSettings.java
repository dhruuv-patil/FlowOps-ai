package com.flowops.integration.delivery.smtp;

import java.time.Duration;

/**
 * Immutable SMTP connection settings derived from an integration's stored config.
 * The password is the only secret and is held strictly in memory for one call.
 *
 * @param host        SMTP server host
 * @param port        SMTP server port
 * @param username    auth username (may be blank when the server needs no auth)
 * @param password    auth password (may be blank)
 * @param encryption  {@code NONE}, {@code STARTTLS}, or {@code SSL}
 * @param timeout     per-operation socket/IO timeout
 * @param fromAddress envelope + header From address
 * @param fromName    optional display name for the From header
 */
public record SmtpSettings(
        String host,
        int port,
        String username,
        String password,
        Encryption encryption,
        Duration timeout,
        String fromAddress,
        String fromName) {

    public enum Encryption {
        NONE,
        STARTTLS,
        SSL;

        public static Encryption fromWire(String value) {
            if (value == null || value.isBlank()) {
                return STARTTLS;
            }
            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException unknown) {
                return STARTTLS;
            }
        }
    }
}