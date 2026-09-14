package com.flowops.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.config.AiProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiServiceClient} against a stub AI service (the JDK's built-in
 * {@link HttpServer} — no external dependency, no running Spring context). They pin the
 * two behaviours the M4 Agent Test Console path depends on, which must never blur:
 *
 * <ul>
 *   <li><b>Reachable + not configured</b> → the client returns
 *       {@code AgentRun(configured=false)} with no output and <em>no exception</em>. The UI
 *       renders the honest "AI is not configured" panel from this 200 — it never fabricates
 *       a completion.</li>
 *   <li><b>Unreachable (or any non-2xx)</b> → the client raises
 *       {@link ErrorCode#AI_SERVICE_ERROR}, which the UI surfaces as
 *       "The AI service is currently unavailable."</li>
 * </ul>
 *
 * <p>The not-configured test also captures the request body the client emits and asserts it
 * matches the AI service's {@code AgentRunRequest} schema ({@code instructions} string plus
 * optional {@code model}/{@code tools}/{@code input}) — proving the payload shape directly,
 * rather than by inspection.
 */
class AiServiceClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Empty token → no {@code X-Service-Token} header, matching the local dev default. */
    private AiProperties propertiesFor(String baseUrl) {
        return new AiProperties(baseUrl, "", Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    private HttpServer startStub(
            String path, int statusCode, String jsonBody, AtomicReference<String> capturedBody)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            if (capturedBody != null) {
                capturedBody.set(new String(request, StandardCharsets.UTF_8));
            }
            byte[] response = jsonBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
        return server;
    }

    private String baseUrlOf(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void reachableButNotConfiguredReturnsHonestResultNotAnError() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        HttpServer server = startStub(
                "/ai/agent/run",
                200,
                "{\"configured\":false,\"output\":null,\"tool_calls\":[],\"model\":null}",
                captured);
        try {
            AiServiceClient client = new AiServiceClient(propertiesFor(baseUrlOf(server)), MAPPER);

            AiServiceClient.AgentRun run =
                    client.runAgent("You are a helpful assistant.", null, List.of(), "");

            // A reachable, unconfigured service is an honest 200 — not an exception.
            assertThat(run.configured()).isFalse();
            assertThat(run.output()).isNull();
            assertThat(run.toolCalls()).isEmpty();

            // The emitted body must match the AI service's AgentRunRequest schema.
            JsonNode sent = MAPPER.readTree(captured.get());
            assertThat(sent.get("instructions").isTextual()).isTrue();
            assertThat(sent.get("instructions").asText()).isEqualTo("You are a helpful assistant.");
            assertThat(sent.get("tools").isArray()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void configuredRunSurfacesOutputAndToolCalls() throws Exception {
        HttpServer server = startStub(
                "/ai/agent/run",
                200,
                "{\"configured\":true,\"output\":\"Hello.\",\"model\":\"gpt-4o-mini\","
                        + "\"tool_calls\":[{\"name\":\"get_current_time\",\"arguments\":{},"
                        + "\"result\":\"2026-08-26T00:00:00Z\"}]}",
                null);
        try {
            AiServiceClient client = new AiServiceClient(propertiesFor(baseUrlOf(server)), MAPPER);

            AiServiceClient.AgentRun run = client.runAgent(
                    "Tell the time.", "gpt-4o-mini", List.of("get_current_time"), "what time is it?");

            assertThat(run.configured()).isTrue();
            assertThat(run.output()).isEqualTo("Hello.");
            assertThat(run.model()).isEqualTo("gpt-4o-mini");
            assertThat(run.toolCalls()).hasSize(1);
            assertThat(run.toolCalls().get(0).name()).isEqualTo("get_current_time");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unreachableServiceBecomesAiServiceError() throws Exception {
        // Bind a server to claim an ephemeral port, capture it, then stop it so the port is
        // closed. Connecting there is refused → IOException → AI_SERVICE_ERROR (never mistaken
        // for the honest "not configured" case, which is a 200).
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String baseUrl = baseUrlOf(server);
        server.stop(0);

        AiServiceClient client = new AiServiceClient(propertiesFor(baseUrl), MAPPER);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> client.runAgent("You are a helpful assistant.", null, List.of(), ""));
        assertThat(ex.code()).isEqualTo(ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void nonSuccessStatusBecomesAiServiceError() throws Exception {
        // A reachable service returning a non-2xx (e.g. a 422) is still surfaced as
        // AI_SERVICE_ERROR: the backend distinguishes only "reachable + configured:false"
        // (a 200) from failure. With the AI service's not-configured-first ordering, a
        // not-configured run no longer produces a 422 here at all.
        HttpServer server = startStub(
                "/ai/agent/run", 422, "{\"detail\":\"instructions must not be empty.\"}", null);
        try {
            AiServiceClient client = new AiServiceClient(propertiesFor(baseUrlOf(server)), MAPPER);

            ApiException ex = assertThrows(
                    ApiException.class, () -> client.runAgent("", null, List.of(), ""));
            assertThat(ex.code()).isEqualTo(ErrorCode.AI_SERVICE_ERROR);
        } finally {
            server.stop(0);
        }
    }
}
