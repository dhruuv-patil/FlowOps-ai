package com.flowops.integration.delivery.smtp;

/**
 * The SMTP seam. Production uses {@link SocketSmtpTransport}; tests inject a fake so
 * no test ever needs a real mail account. Both methods throw {@link SmtpException};
 * secrets never appear in messages.
 */
public interface SmtpTransport {

    /** Opens a connection and authenticates (EHLO + optional STARTTLS + AUTH). */
    void testConnection(SmtpSettings settings) throws SmtpException;

    /** Delivers a message from {@code from} to {@code to} with the given subject and body. */
    void send(SmtpSettings settings, String to, String subject, String body) throws SmtpException;
}