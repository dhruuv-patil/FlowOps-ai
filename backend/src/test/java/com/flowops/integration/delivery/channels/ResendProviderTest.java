package com.flowops.integration.delivery.channels;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.integration.delivery.DeliveryContext;
import com.flowops.integration.delivery.DeliveryResult;
import com.flowops.integration.delivery.NotificationMessage;
import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.DecryptedCredentials;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResendProviderTest {

    private static HttpServer server;
    private static ResendProvider provider;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/domains", exchange -> {
            String auth =
                    exchange.getRequestHeaders()
                            .getFirst("Authorization");

            if (auth == null ||
                    !auth.equals("Bearer test-api-key")) {
                sendJson(
                        exchange,
                        401,
                        "{\"message\":\"Unauthorized\"}"
                );
                return;
            }

            if (!exchange.getRequestMethod().equals("GET")) {
                sendJson(
                        exchange,
                        405,
                        "{\"message\":\"Method Not Allowed\"}"
                );
                return;
            }

            sendJson(
                    exchange,
                    200,
                    "{\"data\":[]}"
            );
        });

        server.createContext("/emails", exchange -> {
            String auth =
                    exchange.getRequestHeaders()
                            .getFirst("Authorization");

            if (auth == null ||
                    !auth.equals("Bearer test-api-key")) {
                sendJson(
                        exchange,
                        401,
                        "{\"message\":\"Unauthorized\"}"
                );
                return;
            }

            if (!exchange.getRequestMethod().equals("POST")) {
                sendJson(
                        exchange,
                        405,
                        "{\"message\":\"Method Not Allowed\"}"
                );
                return;
            }

            sendJson(
                    exchange,
                    200,
                    "{\"id\":\"res-123\"}"
            );
        });

        server.setExecutor(
                Executors.newCachedThreadPool()
        );

        server.start();

        String baseUrl =
                "http://localhost:"
                        + server.getAddress().getPort();

        provider = new ResendProvider(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                baseUrl
        );
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json"
                );

        byte[] bytes =
                json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(bytes);
        }
    }

    private DeliveryContext context() {
        return new DeliveryContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Map.of(),
                new DecryptedCredentials(
                        Map.of(
                                "apiKey",
                                "test-api-key",
                                "fromEmail",
                                "from@example.com"
                        )
                )
        );
    }

    @Test
    void testConnectionSuccess() {

        ConnectionTestResult result =
                provider.testConnection(context());

        assertThat(result.success()).isTrue();
    }

    @Test
    void deliverSuccess() {

        DeliveryResult result =
                provider.deliver(
                        context(),
                        new NotificationMessage(
                                "resend",
                                "Hello",
                                Map.of(
                                        "to",
                                        "to@example.com"
                                )
                        )
                );

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    void deliverFailureMissingTo() {

        DeliveryResult result =
                provider.deliver(
                        context(),
                        new NotificationMessage(
                                "resend",
                                "Hello",
                                Map.of()
                        )
                );

        assertThat(result.success()).isFalse();
        assertThat(result.code())
                .isEqualTo(
                        DeliveryResult.Code.INVALID_CONFIG
                );
    }
}