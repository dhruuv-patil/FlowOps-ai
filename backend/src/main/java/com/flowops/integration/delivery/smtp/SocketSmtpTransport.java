package com.flowops.integration.delivery.smtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A minimal, dependency-free SMTP client built on JSSE sockets. Supports the three
 * encryption modes a generic SMTP integration needs:
 * <ul>
 *   <li>{@code NONE} — plain socket</li>
 *   <li>{@code STARTTLS} — plain connect, STARTTLS upgrade (Gmail 587, Outlook 587)</li>
 *   <li>{@code SSL} — implicit TLS (Gmail 465, Outlook 465)</li>
 * </ul>
 * Authentication uses {@code AUTH PLAIN}, which every common provider (Gmail app
 * passwords, Microsoft 365) accepts. Per-operation timeouts bound every read/write,
 * so a silent server cannot hang a run. The password is never included in any
 * exception message or log line.
 */
@Component
public class SocketSmtpTransport implements SmtpTransport {

    private static final Logger log = LoggerFactory.getLogger(SocketSmtpTransport.class);

    private static final String CRLF = "\r\n";

    @Override
    public void testConnection(SmtpSettings settings) throws SmtpException {
        try (Connection conn = Connection.open(settings)) {
            // Handshake + AUTH PLAIN already succeeded; nothing left to do.
        }
    }

    @Override
    public void send(SmtpSettings settings, String to, String subject, String body)
            throws SmtpException {
        String from = settings.fromAddress();
        try (Connection conn = Connection.open(settings)) {
            check(conn.command("MAIL FROM:<" + from + ">"), "MAIL FROM", "250", false);
            check(conn.command("RCPT TO:<" + to + ">"), "RCPT TO", "250", false);
            check(conn.command("DATA"), "DATA", "354", false);

            try {
                conn.writer().write("From: " + headerFrom(settings) + CRLF);
                conn.writer().write("To: " + to + CRLF);
                conn.writer().write("Subject: "
                        + subject.replace("\r", "").replace("\n", " ") + CRLF);
                conn.writer().write("MIME-Version: 1.0" + CRLF);
                conn.writer().write("Content-Type: text/plain; charset=utf-8" + CRLF);
                conn.writer().write(CRLF);
                conn.writer().write(dotStuff(body));
                conn.writer().write(CRLF + "." + CRLF);
                conn.writer().flush();
            } catch (IOException ioFailure) {
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "Could not write the message to the SMTP server.",
                        true, ioFailure);
            }

            String reply = conn.readReply();
            if (!reply.startsWith("250")) {
                throw new SmtpException(
                        SmtpException.Codes.SEND,
                        "The SMTP server rejected the message data.",
                        false);
            }
            conn.quit();
        }
    }

    private String headerFrom(SmtpSettings settings) {
        if (settings.fromName() == null || settings.fromName().isBlank()) {
            return settings.fromAddress();
        }
        return settings.fromName() + " <" + settings.fromAddress() + ">";
    }

    /** Per RFC 5321, a line beginning with a dot has an extra dot inserted before "." is sent. */
    private String dotStuff(String body) {
        return body.replace("\r\n.", "\r\n..").replace("\n.", "\n..");
    }

    private void check(String reply, String step, String expected, boolean authStep)
            throws SmtpException {
        if (expected.length() == 3 && reply.startsWith(expected)) {
            return;
        }
        if (authStep && (reply.startsWith("535") || reply.startsWith("454"))) {
            throw new SmtpException(
                    SmtpException.Codes.AUTH,
                    "SMTP authentication was rejected.",
                    false);
        }
        throw new SmtpException(
                authStep ? SmtpException.Codes.AUTH : SmtpException.Codes.SEND,
                "SMTP " + step + " was rejected (code "
                        + (reply.length() >= 3 ? reply.substring(0, 3) : "???") + ").",
                authStep);
    }

    // ------------------------------------------------------------------ connection

    /**
     * An open SMTP session: socket + buffered reader/writer. {@code Connection.open}
     * walks the handshake (greeting → EHLO → STARTTLS/SSL → AUTH PLAIN) so both
     * {@code testConnection} and {@code send} converge on an authenticated session.
     */
    private static final class Connection implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        private Connection(Socket socket, BufferedReader reader, BufferedWriter writer) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
        }

        BufferedReader reader() {
            return reader;
        }

        BufferedWriter writer() {
            return writer;
        }

        static Connection open(SmtpSettings settings) throws SmtpException {
            Socket socket = new Socket();
            Duration timeout = settings.timeout();
            boolean sslFirst = settings.encryption() == SmtpSettings.Encryption.SSL;
            try {
                socket.connect(
                        new InetSocketAddress(settings.host(), settings.port()),
                        (int) timeout.toMillis());
                socket.setSoTimeout((int) timeout.toMillis());
            } catch (Exception connectFailed) {
                close(socket);
                throw timeoutOrConnection(settings, connectFailed);
            }

            try {
                if (sslFirst) {
                    SSLSocket ssl = upgradeToTls(socket, settings.host());
                    return finishHandshake(settings, ssl, timeout);
                }
                return finishHandshake(settings, socket, timeout);
            } catch (SmtpException failure) {
                close(socket);
                throw failure;
            } catch (Exception failure) {
                close(socket);
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "SMTP handshake failed.",
                        true, failure);
            }
        }

        private static Connection finishHandshake(
                SmtpSettings settings, Socket socket, Duration timeout) throws SmtpException {
            BufferedReader reader;
            BufferedWriter writer;
            try {
                reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
                writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1));
            } catch (IOException streamFailure) {
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "Could not open SMTP streams.",
                        true, streamFailure);
            }
            Connection conn = new Connection(socket, reader, writer);

            String greeting = conn.readReply();
            if (!greeting.startsWith("220")) {
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "The SMTP server did not return a greeting.",
                        true);
            }

            String ehlo = conn.command("EHLO flowops.local");
            if (!ehlo.startsWith("250")) {
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "The SMTP server rejected EHLO.",
                        true);
            }

            if (settings.encryption() == SmtpSettings.Encryption.STARTTLS
                    && ehlo.contains("STARTTLS")) {
                String startTls = conn.command("STARTTLS");
                if (startTls.startsWith("220")) {
                    try {
                        Socket upgraded = upgradeToTls(socket, settings.host());
                        reader = new BufferedReader(new InputStreamReader(
                                upgraded.getInputStream(), StandardCharsets.ISO_8859_1));
                        writer = new BufferedWriter(new OutputStreamWriter(
                                upgraded.getOutputStream(), StandardCharsets.ISO_8859_1));
                        conn = new Connection(upgraded, reader, writer);
                        upgraded.setSoTimeout((int) timeout.toMillis());
                    } catch (Exception upgradeFailure) {
                        throw new SmtpException(
                                SmtpException.Codes.CONNECTION,
                                "The STARTTLS upgrade failed.",
                                true, upgradeFailure);
                    }
                    String ehlo2 = conn.command("EHLO flowops.local");
                    if (!ehlo2.startsWith("250")) {
                        throw new SmtpException(
                                SmtpException.Codes.CONNECTION,
                                "STARTTLS upgrade succeeded but EHLO failed on the secure channel.",
                                true);
                    }
                } else {
                    throw new SmtpException(
                            SmtpException.Codes.CONNECTION,
                            "The SMTP server rejected STARTTLS.",
                            true);
                }
            }

            conn.authenticate(settings);
            return conn;
        }

        /**
         * Upgrades a plain SMTP connection to TLS. {@code peerHost} must be the DNS
         * name the relay is configured with (e.g. {@code smtp.gmail.com}), never the
         * resolved IP: the HTTPS endpoint-identification algorithm below verifies the
         * server certificate against this name's subject alternative names, and mail
         * servers are issued certificates for hostnames, not addresses. Passing the
         * IP here fails the handshake with {@link SSLPeerUnverifiedException} even
         * though the certificate is perfectly valid.
         */
        private static SSLSocket upgradeToTls(Socket plain, String peerHost) throws Exception {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            SSLSocketFactory factory = context.getSocketFactory();
            SSLSocket ssl = (SSLSocket) factory.createSocket(
                    plain, peerHost, plain.getPort(), true);
            SSLParameters params = ssl.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            ssl.setSSLParameters(params);
            ssl.startHandshake();
            return ssl;
        }

        private void authenticate(SmtpSettings settings) throws SmtpException {
            if (settings.username() == null || settings.username().isBlank()) {
                return; // no auth configured; some relays accept this
            }
            char nul = 0;
            String plain = String.valueOf(nul) + settings.username()
                    + String.valueOf(nul)
                    + (settings.password() == null ? "" : settings.password());
            String auth = "AUTH PLAIN " + Base64.getEncoder().encodeToString(
                    plain.getBytes(StandardCharsets.UTF_8));
            String reply = command(auth);
            if (reply.startsWith("235")) {
                return;
            }
            // 535/454 = rejected; 503 = auth already done; 534 = mechanism not supported.
            if (reply.startsWith("535") || reply.startsWith("454")) {
                throw new SmtpException(
                        SmtpException.Codes.AUTH,
                        "SMTP authentication was rejected by the server.",
                        false);
            }
            throw new SmtpException(
                    SmtpException.Codes.AUTH,
                    "SMTP authentication was not accepted (code "
                            + (reply.length() >= 3 ? reply.substring(0, 3) : "???") + ").",
                    false);
        }

        String command(String line) throws SmtpException {
            try {
                writer.write(line + CRLF);
                writer.flush();
            } catch (IOException ioFailure) {
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "Could not write to the SMTP server.",
                        true, ioFailure);
            }
            return readReply();
        }

        String readReply() throws SmtpException {
            try {
                // A multiline reply has a dash after the code on interim lines; the last
                // line carries a space. Read until we see an un-dashed code line.
                String first = reader.readLine();
                if (first == null) {
                    throw new SmtpException(
                            SmtpException.Codes.CONNECTION,
                            "The SMTP server closed the connection.",
                            true);
                }
                StringBuilder result = new StringBuilder(first);
                if (first.length() >= 4 && first.charAt(3) == '-') {
                    String code = first.substring(0, 3);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(CRLF).append(line);
                        if (line.length() >= 4 && line.startsWith(code) && line.charAt(3) == ' ') {
                            break;
                        }
                    }
                }
                return result.toString();
            } catch (java.net.SocketTimeoutException timeout) {
                throw new SmtpException(
                        SmtpException.Codes.TIMEOUT,
                        "The SMTP server did not respond within the configured timeout.",
                        true, timeout);
            } catch (IOException ioFailure) {
                throw new SmtpException(
                        SmtpException.Codes.CONNECTION,
                        "Could not read from the SMTP server.",
                        true, ioFailure);
            }
        }

        void quit() {
            try {
                writer.write("QUIT" + CRLF);
                writer.flush();
            } catch (IOException ignored) {
                // Best-effort goodbye.
            } finally {
                close(socket);
            }
        }

        @Override
        public void close() {
            close(socket);
        }

        private static void close(Socket socket) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {
                // Closing a broken socket is fine.
            }
        }
    }

    private static SmtpException timeoutOrConnection(SmtpSettings settings, Exception cause) {
        if (cause instanceof java.net.SocketTimeoutException) {
            return new SmtpException(
                    SmtpException.Codes.TIMEOUT,
                    "The SMTP server did not respond within the configured timeout.",
                    true, cause);
        }
        log.info("SMTP connect failed to {}:{} ({})",
                settings.host(), settings.port(), cause.getClass().getSimpleName());
        return new SmtpException(
                SmtpException.Codes.CONNECTION,
                "Could not connect to the SMTP server.",
                true, cause);
    }
}