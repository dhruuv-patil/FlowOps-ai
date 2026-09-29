package com.flowops.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.ai.AiServiceClient;
import com.flowops.config.ExecutionProperties;
import com.flowops.execution.executors.AiAgentExecutor;
import com.flowops.execution.executors.ConditionExecutor;
import com.flowops.execution.executors.DelayExecutor;
import com.flowops.execution.executors.HumanApprovalExecutor;
import com.flowops.execution.executors.ManualTriggerExecutor;
import com.flowops.execution.executors.NotificationExecutor;
import com.flowops.execution.executors.TransformExecutor;
import com.flowops.execution.executors.WebhookTriggerExecutor;
import com.flowops.integration.delivery.DeliveryContext;
import com.flowops.integration.delivery.DeliveryResult;
import com.flowops.integration.delivery.NotificationMessage;
import com.flowops.integration.delivery.NotificationProvider;
import com.flowops.integration.delivery.NotificationProviderRegistry;
import com.flowops.integration.provider.ConnectionTestResult;
import com.flowops.integration.provider.CredentialField;
import com.flowops.integration.provider.IntegrationType;
import com.flowops.workflow.graph.GraphNode;
import com.flowops.workflow.graph.GraphPosition;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Direct unit tests for each {@link NodeExecutor} in the engine. The engine's
 * own contract — retry, backoff, branch resolution, SSE — is covered by the
 * golden-path integration test; these tests pin the per-executor contract
 * (what they read from the config, what they return, what they refuse to do
 * when the config is empty).
 *
 * <p>HTTP Request and Slack Notification are not covered here because they
 * require real network I/O — they are covered by the golden-path integration
 * test with a real local HTTP server.
 */
class ExecutorsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ExecutionProperties PROPERTIES = new ExecutionProperties(
            4,
            3,
            Duration.ofSeconds(2),
            Duration.ofSeconds(30),
            300,
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            1_048_576,
            Duration.ofMinutes(30));

    private GraphNode node(String id, String type, Map<String, Object> config) {
        return new GraphNode(
                id,
                type,
                new GraphPosition(0, 0),
                Map.of("config", config));
    }

    /**
     * Executor wired to a registry of stub delivery providers (slack/email) that
     * declare no usable credentials. Tests exercise the "not delivered, run proceeds"
     * contract without any real channel or network I/O.
     */
    private NotificationExecutor executor() {
        NotificationProvider slack =
                stubProvider(IntegrationType.SLACK, "Slack");

        NotificationProvider email =
                stubProvider(IntegrationType.EMAIL, "Email (SMTP)");

        return new NotificationExecutor(
                new NotificationProviderRegistry(new com.flowops.integration.provider.IntegrationRegistry(List.of(slack, email))));
    }

    private static NotificationProvider stubProvider(
            IntegrationType type,
            String name) {

        return new NotificationProvider() {

            @Override
            public IntegrationType type() {
                return type;
            }

            @Override
            public String displayName() {
                return name;
            }

            @Override
            public String description() {
                return name;
            }

            @Override
            public List<CredentialField> credentialFields() {
                return List.of();
            }

            @Override
            public String secretKey() {
                return null;
            }

            @Override
            public ConnectionTestResult testConnection(
                    DeliveryContext context) {

                return ConnectionTestResult.failure(
                        "no stub delivery");
            }

            @Override
            public DeliveryResult deliver(
                    DeliveryContext context,
                    NotificationMessage message) {

                return DeliveryResult.failure(
                        "NO_PROVIDER",
                        "no stub delivery",
                        false,
                        null,
                        0);
            }

            @Override
            public DeliveryResult sendTest(
                    DeliveryContext context) {

                return DeliveryResult.failure(
                        "NO_PROVIDER",
                        "no stub delivery",
                        false,
                        null,
                        0);
            }
        };
    }

    private NodeExecutionContext ctx(
            GraphNode n,
            Map<String, Object> config,
            ObjectNode variables) {

        return new NodeExecutionContext(
                n,
                config,
                Map.of(),
                variables,
                new VariableInterpolator(),
                MAPPER,
                PROPERTIES,
                noopLogger());
    }

    private NodeLogger noopLogger() {
        return new NodeLogger() {

            @Override
            public void debug(String message) {
            }

            @Override
            public void info(String message) {
            }

            @Override
            public void warn(String message) {
            }

            @Override
            public void error(String message) {
            }
        };
    }

    // ---- ManualTriggerExecutor / WebhookTriggerExecutor --------------------

    @Test
    void manualTriggerForwardsItsPayload() throws Exception {
        ObjectNode trigger = MAPPER.createObjectNode();
        trigger.put("foo", "bar");

        ObjectNode variables = MAPPER.createObjectNode();
        variables.set("trigger", trigger);

        NodeResult result = new ManualTriggerExecutor().execute(
                ctx(
                        node("t", "manual_trigger", Map.of()),
                        Map.of(),
                        variables));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output().get("foo").asText())
                .isEqualTo("bar");
    }

    @Test
    void webhookTriggerForwardsItsPayload() throws Exception {
        ObjectNode trigger = MAPPER.createObjectNode();
        trigger.put("event", "pull_request");

        ObjectNode variables = MAPPER.createObjectNode();
        variables.set("trigger", trigger);

        NodeResult result = new WebhookTriggerExecutor().execute(
                ctx(
                        node("t", "webhook_trigger", Map.of()),
                        Map.of(),
                        variables));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output().get("event").asText())
                .isEqualTo("pull_request");
    }

    // ---- ConditionExecutor -------------------------------------------------

    @Test
    void conditionEvaluatesTrueExpressionAndFiresTheTruePort()
            throws Exception {

        ObjectNode variables = MAPPER.createObjectNode();
        variables.putObject("http").put("status", 200);

        NodeResult result =
                new ConditionExecutor(new ExpressionEvaluator()).execute(
                        ctx(
                                node(
                                        "c",
                                        "condition",
                                        Map.of(
                                                "expression",
                                                "{{http.status}} == 200")),
                                Map.of(
                                        "expression",
                                        "{{http.status}} == 200"),
                                variables));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.handles())
                .containsExactly("true");

        assertThat(result.output().get("result").asBoolean())
                .isTrue();
    }

    @Test
    void conditionEvaluatesFalseExpressionAndFiresTheFalsePort()
            throws Exception {

        ObjectNode variables = MAPPER.createObjectNode();
        variables.putObject("http").put("status", 500);

        NodeResult result =
                new ConditionExecutor(new ExpressionEvaluator()).execute(
                        ctx(
                                node(
                                        "c",
                                        "condition",
                                        Map.of(
                                                "expression",
                                                "{{http.status}} == 200")),
                                Map.of(
                                        "expression",
                                        "{{http.status}} == 200"),
                                variables));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.handles())
                .containsExactly("false");

        assertThat(result.output().get("result").asBoolean())
                .isFalse();
    }

    @Test
    void conditionFailsWithAnEmptyExpression() throws Exception {
        NodeResult result =
                new ConditionExecutor(new ExpressionEvaluator()).execute(
                        ctx(
                                node("c", "condition", Map.of()),
                                Map.of(),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.FAIL);

        assertThat(result.error())
                .contains("no expression");
    }

    // ---- TransformExecutor -------------------------------------------------

    @Test
    void transformInterpolatesAndCoercesScalarFields()
            throws Exception {

        ObjectNode variables = MAPPER.createObjectNode();
        variables.putObject("http").put("status", 200);

        Map<String, Object> config = Map.of(
                "mapping",
                Map.of(
                        "status",
                        "{{http.status}}",
                        "ok",
                        "{{http.status}} == 200",
                        "name",
                        "literal"));

        NodeResult result =
                new TransformExecutor().execute(
                        ctx(
                                node("t", "transform", config),
                                config,
                                variables));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output().get("status").asInt())
                .isEqualTo(200);

        assertThat(result.output().get("ok").asBoolean())
                .isTrue();

        assertThat(result.output().get("name").asText())
                .isEqualTo("literal");
    }

    @Test
    void transformFailsWhenTheMappingIsMissing()
            throws Exception {

        NodeResult result =
                new TransformExecutor().execute(
                        ctx(
                                node("t", "transform", Map.of()),
                                Map.of(),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.FAIL);
    }

    // ---- DelayExecutor -----------------------------------------------------

    @Test
    void delayWaitsTheRequestedSeconds() throws Exception {

        Map<String, Object> config =
                Map.of("seconds", 1);

        long started = System.nanoTime();

        NodeResult result =
                new DelayExecutor().execute(
                        ctx(
                                node("d", "delay", config),
                                config,
                                MAPPER.createObjectNode()));

        long elapsedMs =
                (System.nanoTime() - started) / 1_000_000;

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output()
                .get("requestedSeconds")
                .asDouble())
                .isEqualTo(1.0);

        assertThat(result.output()
                .get("waitedSeconds")
                .asDouble())
                .isEqualTo(1.0);

        // Allow generous slop for CI scheduling jitter.
        assertThat(elapsedMs)
                .isBetween(900L, 5_000L);
    }

    @Test
    void delayCapsAtTheConfiguredMaximum()
            throws Exception {

        /*
         * Do NOT use the production PROPERTIES here.
         *
         * Production maxDelaySeconds = 300, which would make this
         * unit test sleep for 5 minutes.
         *
         * Use a small test-specific maximum so the test validates
         * the exact same behavior without introducing a 5-minute
         * test-suite delay.
         */
        ExecutionProperties testProperties =
                new ExecutionProperties(
                        4,
                        3,
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(30),
                        2,
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(30),
                        1_048_576,
                        Duration.ofMinutes(30));

        Map<String, Object> config =
                Map.of("seconds", 999);

        GraphNode delayNode =
                node("d", "delay", config);

        NodeExecutionContext context =
                new NodeExecutionContext(
                        delayNode,
                        config,
                        Map.of(),
                        MAPPER.createObjectNode(),
                        new VariableInterpolator(),
                        MAPPER,
                        testProperties,
                        noopLogger());

        long started = System.nanoTime();

        NodeResult result =
                new DelayExecutor().execute(context);

        long elapsedMs =
                (System.nanoTime() - started) / 1_000_000;

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output()
                .get("cappedAtSeconds")
                .asLong())
                .isEqualTo(2L);

        assertThat(result.output()
                .get("waitedSeconds")
                .asLong())
                .isEqualTo(2L);

        // The test should now take only about 2 seconds,
        // not the production 300 seconds.
        assertThat(elapsedMs)
                .isBetween(1_500L, 5_000L);
    }

    // ---- HumanApprovalExecutor --------------------------------------------

    @Test
    void humanApprovalAlwaysReturnsWaiting()
            throws Exception {

        NodeResult result =
                new HumanApprovalExecutor().execute(
                        ctx(
                                node(
                                        "h",
                                        "human_approval",
                                        Map.of(
                                                "prompt",
                                                "ship it?")),
                                Map.of(
                                        "prompt",
                                        "ship it?"),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.WAIT);
    }

    // ---- AiAgentExecutor ---------------------------------------------------

    @Test
    void aiAgentWithNoInstructionsSucceedsWithConfiguredFalse()
            throws Exception {

        AiServiceClient client =
                Mockito.mock(AiServiceClient.class);

        NodeResult result =
                new AiAgentExecutor(client).execute(
                        ctx(
                                node("a", "ai_agent", Map.of()),
                                Map.of(),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output()
                .get("configured")
                .asBoolean())
                .isFalse();

        assertThat(result.output()
                .has("note"))
                .isTrue();
    }

    @Test
    void aiAgentWithInstructionsPropagatesTheServiceConfiguredFlag()
            throws Exception {

        AiServiceClient client =
                Mockito.mock(AiServiceClient.class);

        Mockito.when(
                client.runAgent(
                        Mockito.eq("be helpful"),
                        Mockito.isNull(),
                        Mockito.anyList(),
                        Mockito.anyString()))
                .thenReturn(
                        new AiServiceClient.AgentRun(
                                false,
                                null,
                                List.of(),
                                null));

        NodeResult result =
                new AiAgentExecutor(client).execute(
                        ctx(
                                node(
                                        "a",
                                        "ai_agent",
                                        Map.of(
                                                "instructions",
                                                "be helpful",
                                                "input",
                                                "hi")),
                                Map.of(
                                        "instructions",
                                        "be helpful",
                                        "input",
                                        "hi"),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output()
                .get("configured")
                .asBoolean())
                .isFalse();

        // The output is empty when the service is not configured,
        // never fabricated.
        assertThat(result.output()
                .get("output")
                .asText())
                .isEmpty();
    }

    // ---- NotificationExecutor ----------------------------------------------

    @Test
    void notificationWithNoChannelFails()
            throws Exception {

        NodeResult result =
                executor().execute(
                        ctx(
                                node(
                                        "n",
                                        "notification",
                                        Map.of("message", "hi")),
                                Map.of("message", "hi"),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.FAIL);

        assertThat(result.error())
                .contains("channel");
    }

    @Test
    void notificationWithUnconnectedSlackStaysHonest()
            throws Exception {

        // No secret in the context → "not delivered", but the run still succeeds
        // so downstream nodes can run. The executor must never fake a delivery.
        NodeResult result =
                executor().execute(
                        ctx(
                                node(
                                        "n",
                                        "notification",
                                        Map.of(
                                                "channel",
                                                "slack",
                                                "message",
                                                "hi")),
                                Map.of(
                                        "channel",
                                        "slack",
                                        "message",
                                        "hi"),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output()
                .get("delivered")
                .asBoolean())
                .isFalse();

        assertThat(result.output()
                .get("reason")
                .asText())
                .contains("slack");
    }

    @Test
    void notificationWithAnUnconnectedEmailChannelStaysHonest()
            throws Exception {

        NodeResult result =
                executor().execute(
                        ctx(
                                node(
                                        "n",
                                        "notification",
                                        Map.of(
                                                "channel",
                                                "email",
                                                "message",
                                                "hi")),
                                Map.of(
                                        "channel",
                                        "email",
                                        "message",
                                        "hi"),
                                MAPPER.createObjectNode()));

        assertThat(result.kind())
                .isEqualTo(NodeResult.Kind.SUCCESS);

        assertThat(result.output()
                .get("delivered")
                .asBoolean())
                .isFalse();
    }
}