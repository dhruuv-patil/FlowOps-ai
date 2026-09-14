package com.flowops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Operator-level SMTP configuration for the password-reset mailer
 * ({@code flowops.mail}).
 *
 * <p>Independent of the customer-facing <em>email integration</em>
 * ({@code IntegrationType.EMAIL}), whose relay is configured per organization at
 * runtime and stored encrypted: this block is the app's own outbound relay for
 * system email. When {@code host} is left empty the mailer is disabled and
 * forgot-password behaves exactly as before — a token is still minted and stored,
 * but nothing is delivered.
 *
 * <p>{@code password} is a secret. It must never appear in a log line or an API
 * response, and this file never ships a real value — operators supply it by
 * environment variable.
 *
 * @param host        SMTP relay host; empty disables password-reset mail
 * @param port        SMTP relay port (587 STARTTLS, 465 SSL)
 * @param username    AUTH PLAIN username (may be blank for open relays)
 * @param password    AUTH PLAIN password (secret — never logged)
 * @param encryption  {@code NONE}, {@code STARTTLS}, or {@code SSL}
 * @param fromAddress envelope + From-header address
 * @param fromName    display name shown beside the From address
 * @param appBaseUrl  frontend origin used to build the reset link, e.g.
 *                    {@code https://app.flowops.example.com}
 */
@ConfigurationProperties(prefix = "flowops.mail")
public record MailProperties(
        @DefaultValue("") String host,
        @DefaultValue("587") int port,
        @DefaultValue("") String username,
        @DefaultValue("") String password,
        @DefaultValue("STARTTLS") String encryption,
        @DefaultValue("") String fromAddress,
        @DefaultValue("FlowOps") String fromName,
        @DefaultValue("http://localhost:3000") String appBaseUrl) {

    public MailProperties {
        host = clean(host);
        username = clean(username);
        fromAddress = clean(fromAddress);
        fromName = clean(fromName);
        encryption = clean(encryption);
        appBaseUrl = stripTrailingSlashes(appBaseUrl);
    }

    /** A send is only possible when a relay host and a From address are configured. */
    public boolean isConfigured() {
        return !host.isBlank() && !fromAddress.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static String stripTrailingSlashes(String url) {
        String value = clean(url);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}