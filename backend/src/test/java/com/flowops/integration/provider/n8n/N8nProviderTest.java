package com.flowops.integration.provider.n8n;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.DecryptedCredentials;
import com.flowops.integration.provider.ExecutionPage;
import com.flowops.integration.provider.ExternalExecution;
import com.flowops.integration.provider.ExternalNodeExecution;
import com.flowops.integration.provider.ExternalWorkflow;
import com.flowops.integration.provider.IntegrationContext;
import com.flowops.integration.provider.SyncCursor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link N8nProvider} using a real embedded HTTP server.
 */
class N8nProviderTest {

    private static HttpServer server;
    private static String baseUrl;
    private static N8nProvider provider;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;

        /*
         * Normal n8n workflow endpoint.
         */
        server.createContext("/api/v1/workflows", exchange -> {
            String auth =
                    exchange.getRequestHeaders()
                            .getFirst("X-N8N-API-KEY");

            if (auth == null || !auth.equals("test-api-key")) {
                sendJson(
                        exchange,
                        401,
                        "{\"message\":\"Unauthorized\"}");
                return;
            }

            String json =
                    "{\"data\":[{\"id\":\"wf-1\","
                            + "\"name\":\"Test Workflow\","
                            + "\"active\":true,"
                            + "\"updatedAt\":\"2024-01-15T10:30:00.000Z\"}]}";

            sendJson(exchange, 200, json);
        });

        /*
         * Normal n8n executions endpoint.
         */
        server.createContext("/api/v1/executions", exchange -> {
            String auth =
                    exchange.getRequestHeaders()
                            .getFirst("X-N8N-API-KEY");

            if (auth == null || !auth.equals("test-api-key")) {
                sendJson(
                        exchange,
                        401,
                        "{\"message\":\"Unauthorized\"}");
                return;
            }

            String json =
                    "{\"data\":[{\"id\":\"exec-1\","
                            + "\"workflowId\":\"wf-1\","
                            + "\"status\":\"success\","
                            + "\"startedAt\":\"2024-01-15T10:00:00.000Z\","
                            + "\"stoppedAt\":\"2024-01-15T10:01:30.000Z\","
                            + "\"mode\":\"manual\"}]}";

            sendJson(exchange, 200, json);
        });

        /*
         * n8n execution detail endpoint.
         */
        server.createContext(
                "/api/v1/executions/exec-1",
                exchange -> {
                    String auth =
                            exchange.getRequestHeaders()
                                    .getFirst("X-N8N-API-KEY");

                    if (auth == null || !auth.equals("test-api-key")) {
                        sendJson(
                                exchange,
                                401,
                                "{\"message\":\"Unauthorized\"}");
                        return;
                    }

                    String json =
                            """
                            {
                              "data": {
                                "resultData": {
                                  "runData": {
                                    "HTTP Request": [
                                      {
                                        "startTime": 1705312800000,
                                        "executionTime": 1500,
                                        "executionStatus": "success",
                                        "error": null,
                                        "outputSize": 2048
                                      }
                                    ]
                                  }
                                },
                                "status": "success"
                              }
                            }
                            """;

                    sendJson(exchange, 200, json);
                });

        /*
         * Error endpoints.
         */
        server.createContext(
                "/404/api/v1/workflows",
                exchange ->
                        sendJson(
                                exchange,
                                404,
                                "{\"message\":\"Not found\"}"));

        server.createContext(
                "/500/api/v1/workflows",
                exchange ->
                        sendJson(
                                exchange,
                                500,
                                "{\"message\":\"Internal error\"}"));

        server.createContext(
                "/429/api/v1/workflows",
                exchange -> {
                    exchange.getResponseHeaders()
                            .add("Retry-After", "60");

                    sendJson(
                            exchange,
                            429,
                            "{\"message\":\"Rate limited\"}");
                });

        server.setExecutor(
                Executors.newCachedThreadPool());

        server.start();

        N8nClient client =
                new N8nClient(new ObjectMapper());

        provider =
                new N8nProvider(client);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String json) throws IOException {

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json");

        byte[] bytes =
                json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status,
                bytes.length);

        try (OutputStream os =
                     exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private IntegrationContext context() {
        Map<String, String> creds =
                Map.of(
                        "baseUrl",
                        baseUrl,
                        "apiKey",
                        "test-api-key");

        return new IntegrationContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new DecryptedCredentials(creds));
    }

    @Test
    void typeIsN8N() {
        assertThat(provider.type())
                .isEqualTo(
                        com.flowops.integration.provider.IntegrationType.N8N);
    }

    @Test
    void testConnectionSuccess() {
        ConnectionTestResult result =
                provider.testConnection(context());

        assertThat(result.success())
                .isTrue();
    }

    @Test
    void testConnectionAuthFailure() {
        Map<String, String> creds =
                Map.of(
                        "baseUrl",
                        baseUrl,
                        "apiKey",
                        "wrong-key");

        IntegrationContext ctx =
                new IntegrationContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new DecryptedCredentials(creds));

        ConnectionTestResult result =
                provider.testConnection(ctx);

        assertThat(result.success())
                .isFalse();

        assertThat(result.message())
                .contains("Authentication");
    }

    @Test
    void testConnectionConfigError404() {
        Map<String, String> creds =
                Map.of(
                        "baseUrl",
                        baseUrl + "/404",
                        "apiKey",
                        "test-api-key");

        IntegrationContext ctx =
                new IntegrationContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new DecryptedCredentials(creds));

        ConnectionTestResult result =
                provider.testConnection(ctx);

        assertThat(result.success())
                .isFalse();

        assertThat(result.message())
                .contains("Configuration");
    }

    @Test
    void testConnectionUnavailable500() {
        Map<String, String> creds =
                Map.of(
                        "baseUrl",
                        baseUrl + "/500",
                        "apiKey",
                        "test-api-key");

        IntegrationContext ctx =
                new IntegrationContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new DecryptedCredentials(creds));

        ConnectionTestResult result =
                provider.testConnection(ctx);

        assertThat(result.success())
                .isFalse();

        assertThat(result.message())
                .contains("unavailable");
    }

    @Test
    void discoverWorkflows() {
        List<ExternalWorkflow> workflows =
                provider.discoverWorkflows(context());

        assertThat(workflows)
                .hasSize(1);

        assertThat(workflows.get(0).externalId())
                .isEqualTo("wf-1");

        assertThat(workflows.get(0).name())
                .isEqualTo("Test Workflow");

        assertThat(workflows.get(0).status())
                .isEqualTo("active");
    }

    @Test
    void fetchExecutionsWithCursor() {
        ExecutionPage<ExternalExecution> page =
                provider.fetchExecutions(
                        context(),
                        SyncCursor.EMPTY);

        assertThat(page.items())
                .hasSize(1);

        assertThat(page.items().get(0).externalId())
                .isEqualTo("exec-1");

        assertThat(page.items().get(0).workflowExternalId())
                .isEqualTo("wf-1");

        assertThat(page.nextCursor())
                .isEqualTo(SyncCursor.EMPTY);

        assertThat(page.hasMore())
                .isFalse();
    }

    @Test
    void fetchNodeExecutions() {
        List<ExternalExecution> executions =
                provider.fetchExecutions(
                        context(),
                        SyncCursor.EMPTY)
                        .items();

        ExternalExecution execution =
                executions.get(0);

        List<ExternalNodeExecution> nodes =
                provider.fetchNodeExecutions(
                        context(),
                        execution);

        assertThat(nodes)
                .hasSize(1);

        assertThat(nodes.get(0).externalId())
                .isEqualTo("HTTP Request");

        assertThat(nodes.get(0).name())
                .isEqualTo("HTTP Request");

        assertThat(nodes.get(0).status())
                .isEqualTo(
                        com.flowops.integration.provider.ExecutionStatus.SUCCEEDED);

        assertThat(nodes.get(0).startedAt())
                .isEqualTo(
                        java.time.Instant.ofEpochMilli(
                                1705312800000L));

        assertThat(nodes.get(0).finishedAt())
                .isEqualTo(
                        java.time.Instant.ofEpochMilli(
                                1705312801500L));

        assertThat(nodes.get(0).outputSize())
                .isEqualTo(2048L);
    }

    @Test
    void capabilitiesFlags() {
        var capabilities =
                provider.capabilities();

        assertThat(capabilities.workflowDiscovery())
                .isTrue();

        assertThat(capabilities.executionHistory())
                .isTrue();

        assertThat(capabilities.nodeExecutionData())
                .isTrue();

        assertThat(capabilities.incrementalSync())
                .isTrue();

        assertThat(capabilities.webhooks())
                .isTrue();

        assertThat(capabilities.tracing())
                .isFalse();
    }
}