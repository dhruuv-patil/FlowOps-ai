package com.flowops.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flowops.api.AgentRunResponse;
import com.flowops.config.AiProperties;
import com.flowops.domain.AiAgent;
import com.flowops.domain.Role;
import com.flowops.repository.AiAgentRepository;
import com.flowops.security.FlowOpsPrincipal;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the <em>actual</em> agent-run path behind
 * {@code POST /api/ai/agents/{agentId}/run}:
 * {@code AiAgentController.run → AiAgentService.testRun → toolList → AiServiceClient.runAgent}.
 *
 * <p>It drives {@code testRun} against a stub AI service (the JDK's {@link HttpServer}, no
 * external dependency) and captures the exact bytes the backend sends, asserting:
 *
 * <ul>
 *   <li>the serialized body matches the AI service's {@code AgentRunRequest} schema —
 *       {@code instructions} (non-empty string), {@code model} (absent or string),
 *       {@code tools} (array of strings, never null), {@code input} (absent or string) — so
 *       the real path can never provoke a 422;</li>
 *   <li>a {@code configured:false} response (the no-provider-key case) is propagated as a
 *       normal {@link AgentRunResponse}, never converted into an error/502.</li>
 * </ul>
 */
class AgentRunPathTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NOT_CONFIGURED_BODY =
            "{\"configured\":false,\"output\":null,\"tool_calls\":[],\"model\":null}";

    private HttpServer startStub(int statusCode, String body, AtomicReference<String> captured)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/agent/run", exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
        return server;
    }

    private AiAgentService serviceTalkingTo(HttpServer server, AiAgent agent) {
        AiAgentRepository repo = mock(AiAgentRepository.class);
        when(repo.findByIdAndOrganizationId(any(), any())).thenReturn(Optional.of(agent));
        AiProperties props = new AiProperties(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5));
        return new AiAgentService(repo, new AiServiceClient(props, MAPPER), MAPPER);
    }

    private AiAgent agent(UUID orgId, String model, String... tools) {
        ArrayNode toolsJson = MAPPER.createArrayNode();
        for (String t : tools) {
            toolsJson.add(t);
        }
        return AiAgent.create(
                orgId,
                UUID.randomUUID(),
                "Test agent",
                "For the regression test.",
                "You are a helpful assistant.",
                model,
                toolsJson);
    }

    private FlowOpsPrincipal principalIn(UUID orgId) {
        return new FlowOpsPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), orgId, Role.OWNER, "owner@example.com");
    }

    @Test
    void testRunSendsSchemaValidBodyAndPropagatesNotConfigured() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        HttpServer server = startStub(200, NOT_CONFIGURED_BODY, captured);
        try {
            UUID orgId = UUID.randomUUID();
            AiAgentService service =
                    serviceTalkingTo(server, agent(orgId, "gpt-4o-mini", "get_current_time", "calculate"));

            AgentRunResponse response =
                    service.testRun(principalIn(orgId), UUID.randomUUID(), new AgentTestRequest("Hello"));

            // configured:false comes back as a normal response — never an exception/502.
            assertThat(response.configured()).isFalse();
            assertThat(response.output()).isNull();

            // The serialized body must satisfy the AI service's AgentRunRequest schema.
            JsonNode sent = MAPPER.readTree(captured.get());
            assertThat(sent.get("instructions").isTextual()).isTrue();
            assertThat(sent.get("instructions").asText()).isNotBlank();

            assertThat(sent.get("tools").isArray()).isTrue();
            assertThat(sent.get("tools").size()).isEqualTo(2);
            for (JsonNode tool : sent.get("tools")) {
                assertThat(tool.isTextual()).as("every tool is a string").isTrue();
            }

            if (sent.has("model")) {
                assertThat(sent.get("model").isTextual()).isTrue();
            }
            if (sent.has("input")) {
                assertThat(sent.get("input").isTextual()).isTrue();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testRunWithNoToolsSendsEmptyArrayNeverNull() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        HttpServer server = startStub(200, NOT_CONFIGURED_BODY, captured);
        try {
            UUID orgId = UUID.randomUUID();
            // No model, no tools, no input — the minimal agent. The body must still be valid.
            AiAgentService service = serviceTalkingTo(server, agent(orgId, null));

            service.testRun(principalIn(orgId), UUID.randomUUID(), new AgentTestRequest(null));

            JsonNode sent = MAPPER.readTree(captured.get());
            assertThat(sent.get("instructions").isTextual()).isTrue();
            // tools must be an empty array, never JSON null (null would fail list[str]).
            assertThat(sent.get("tools").isArray()).isTrue();
            assertThat(sent.get("tools").size()).isZero();
            // model was null → omitted entirely rather than sent as null.
            assertThat(sent.hasNonNull("model")).isFalse();
        } finally {
            server.stop(0);
        }
    }
}
