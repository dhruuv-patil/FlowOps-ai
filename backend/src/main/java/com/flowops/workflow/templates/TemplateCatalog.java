package com.flowops.workflow.templates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The built-in template gallery.
 *
 * <p>Every graph here is assembled from {@code NodeRegistry} node types with all
 * required config fields filled in, so a template lands in the builder ready to
 * validate and publish rather than as a broken skeleton the user has to repair.
 * {@code WorkflowTemplateCatalogTest} asserts exactly that against the real
 * validator, which is what keeps this catalogue honest as the registry evolves.
 *
 * <p>Graphs are built once at startup and handed out as {@code deepCopy()} by
 * {@link TemplateService}, so a caller can never mutate the shared instance.
 */
@Component
public class TemplateCatalog {

    private final Map<String, WorkflowTemplate> bySlug = new LinkedHashMap<>();
    private final ObjectMapper mapper;

    public TemplateCatalog(ObjectMapper mapper) {
        this.mapper = mapper;
        register(apiHealthCheck());
        register(webhookAiSummary());
        register(approvalBeforeAction());
        register(webhookTransformForward());
        register(delayedFollowUp());
        register(aiTriageInbound());
    }

    private void register(WorkflowTemplate template) {
        bySlug.put(template.slug(), template);
    }

    /** All templates in a stable, gallery-friendly order. */
    public List<WorkflowTemplate> all() {
        return List.copyOf(bySlug.values());
    }

    public Optional<WorkflowTemplate> find(String slug) {
        return Optional.ofNullable(slug == null ? null : bySlug.get(slug.strip()));
    }

    /* ------------------------------------------------------------ templates */

    private WorkflowTemplate apiHealthCheck() {
        Graph g = graph();
        g.node("trigger", "manual_trigger", "Run health check", Map.of(), 0, 1);
        g.node("check", "http_request", "Ping the endpoint", Map.of(
                "method", "GET",
                "url", "https://example.com/health"), 1, 1);
        g.node("gate", "condition", "Healthy?", Map.of(
                "expression", "{{check.status}} == 200"), 2, 1);
        g.node("ok", "notification", "Report healthy", Map.of(
                "channel", "slack",
                "message", "Health check passed with status {{check.status}}."), 3, 0);
        g.node("alert", "notification", "Raise an alert", Map.of(
                "channel", "slack",
                "message", "Health check FAILED with status {{check.status}}."), 3, 2);
        g.edge("trigger", "check", null);
        g.edge("check", "gate", null);
        g.edge("gate", "ok", "true");
        g.edge("gate", "alert", "false");
        return new WorkflowTemplate(
                "api-health-check",
                "API health check with Slack alert",
                "Call an endpoint, branch on its status code, and post the outcome to Slack.",
                "Monitoring",
                "Activity",
                List.of("http", "monitoring", "slack", "alert"),
                g.build());
    }

    private WorkflowTemplate webhookAiSummary() {
        Graph g = graph();
        g.node("trigger", "webhook_trigger", "Inbound payload", Map.of("method", "POST"), 0, 1);
        g.node("summarize", "ai_agent", "Summarize the payload", Map.of(
                "instructions", "You summarize incoming JSON payloads in two sentences, "
                        + "plainly and without speculation.",
                "input", "{{trigger.body}}"), 1, 1);
        g.node("post", "notification", "Share the summary", Map.of(
                "channel", "slack",
                "message", "New inbound payload:\n{{summarize.output}}"), 2, 1);
        g.edge("trigger", "summarize", null);
        g.edge("summarize", "post", null);
        return new WorkflowTemplate(
                "webhook-ai-summary",
                "Summarize an inbound webhook with AI",
                "Receive a webhook, have an AI agent summarize the payload, and post it to Slack.",
                "AI",
                "Sparkles",
                List.of("webhook", "ai", "summary", "slack"),
                g.build());
    }

    private WorkflowTemplate approvalBeforeAction() {
        Graph g = graph();
        g.node("trigger", "manual_trigger", "Start request", Map.of(), 0, 1);
        g.node("approval", "human_approval", "Wait for sign-off", Map.of(
                "prompt", "Approve calling the downstream API for this request?"), 1, 1);
        g.node("act", "http_request", "Perform the action", Map.of(
                "method", "POST",
                "url", "https://example.com/api/action",
                "body", "{\"approved\": true}"), 2, 0);
        g.node("declined", "notification", "Tell the requester", Map.of(
                "channel", "email",
                "message", "Your request was reviewed and not approved."), 2, 2);
        g.edge("trigger", "approval", null);
        g.edge("approval", "act", "approved");
        g.edge("approval", "declined", "rejected");
        return new WorkflowTemplate(
                "approval-before-action",
                "Human approval before an action",
                "Pause for a human decision, then either call the API or notify the requester.",
                "Approvals",
                "UserCheck",
                List.of("approval", "human", "gate", "http"),
                g.build());
    }

    private WorkflowTemplate webhookTransformForward() {
        Graph g = graph();
        g.node("trigger", "webhook_trigger", "Inbound payload", Map.of("method", "POST"), 0, 1);
        g.node("shape", "transform", "Reshape the fields", Map.of(
                "mapping", Map.of(
                        "id", "{{trigger.body.id}}",
                        "email", "{{trigger.body.email}}")), 1, 1);
        g.node("forward", "http_request", "Forward downstream", Map.of(
                "method", "POST",
                "url", "https://example.com/api/contacts",
                "headers", Map.of("content-type", "application/json"),
                "body", "{\"id\": \"{{shape.id}}\", \"email\": \"{{shape.email}}\"}"), 2, 1);
        g.edge("trigger", "shape", null);
        g.edge("shape", "forward", null);
        return new WorkflowTemplate(
                "webhook-transform-forward",
                "Reshape a webhook and forward it",
                "Take an inbound webhook, map its fields into a new shape, and POST it onward.",
                "Integrations",
                "Shuffle",
                List.of("webhook", "transform", "http", "integration"),
                g.build());
    }

    private WorkflowTemplate delayedFollowUp() {
        Graph g = graph();
        g.node("trigger", "manual_trigger", "Start follow-up", Map.of(), 0, 1);
        g.node("wait", "delay", "Wait a minute", Map.of("seconds", 60), 1, 1);
        g.node("fetch", "http_request", "Re-check the record", Map.of(
                "method", "GET",
                "url", "https://example.com/api/records/1"), 2, 1);
        g.node("tell", "notification", "Report the result", Map.of(
                "channel", "slack",
                "message", "Follow-up check returned status {{fetch.status}}."), 3, 1);
        g.edge("trigger", "wait", null);
        g.edge("wait", "fetch", null);
        g.edge("fetch", "tell", null);
        return new WorkflowTemplate(
                "delayed-follow-up",
                "Delayed follow-up check",
                "Wait a fixed period, re-read a remote record, and report what changed.",
                "Monitoring",
                "Clock",
                List.of("delay", "http", "follow-up", "slack"),
                g.build());
    }

    private WorkflowTemplate aiTriageInbound() {
        Graph g = graph();
        g.node("trigger", "webhook_trigger", "Inbound ticket", Map.of("method", "POST"), 0, 1);
        g.node("triage", "ai_agent", "Classify urgency", Map.of(
                "instructions", "Classify the ticket's urgency. Reply with exactly one word: "
                        + "URGENT or NORMAL.",
                "input", "{{trigger.body}}"), 1, 1);
        g.node("gate", "condition", "Urgent?", Map.of(
                "expression", "{{triage.output}} == URGENT"), 2, 1);
        g.node("page", "notification", "Page the on-call", Map.of(
                "channel", "slack",
                "message", "URGENT ticket needs attention:\n{{trigger.body}}"), 3, 0);
        g.node("queue", "notification", "Queue it normally", Map.of(
                "channel", "email",
                "message", "A new ticket was filed and queued for normal handling."), 3, 2);
        g.edge("trigger", "triage", null);
        g.edge("triage", "gate", null);
        g.edge("gate", "page", "true");
        g.edge("gate", "queue", "false");
        return new WorkflowTemplate(
                "ai-triage-inbound",
                "AI triage for inbound tickets",
                "Let an AI agent classify an inbound ticket, then page or queue it accordingly.",
                "AI",
                "Bot",
                List.of("ai", "triage", "webhook", "condition"),
                g.build());
    }

    /* -------------------------------------------------------- graph builder */

    private Graph graph() {
        return new Graph(mapper);
    }

    /**
     * Tiny builder for the stored graph shape ({@code data.label} +
     * {@code data.config}, positions on a column/row grid) so each template above
     * reads as its own pipeline rather than as JSON plumbing.
     */
    private static final class Graph {

        private static final int COLUMN_WIDTH = 280;
        private static final int ROW_HEIGHT = 140;

        private final ObjectMapper mapper;
        private final ArrayNode nodes;
        private final ArrayNode edges;

        private Graph(ObjectMapper mapper) {
            this.mapper = mapper;
            this.nodes = mapper.createArrayNode();
            this.edges = mapper.createArrayNode();
        }

        void node(String id, String type, String label, Map<String, Object> config, int column, int row) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", id);
            node.put("type", type);
            ObjectNode position = node.putObject("position");
            position.put("x", 80 + column * COLUMN_WIDTH);
            position.put("y", 80 + row * ROW_HEIGHT);
            ObjectNode data = node.putObject("data");
            data.put("label", label);
            data.set("config", mapper.valueToTree(config));
            nodes.add(node);
        }

        void edge(String source, String target, String sourceHandle) {
            ObjectNode edge = mapper.createObjectNode();
            edge.put("id", "e-" + source + "-" + target + (sourceHandle == null ? "" : "-" + sourceHandle));
            edge.put("source", source);
            edge.put("target", target);
            if (sourceHandle == null) {
                edge.putNull("sourceHandle");
            } else {
                edge.put("sourceHandle", sourceHandle);
            }
            edges.add(edge);
        }

        JsonNode build() {
            ObjectNode graph = mapper.createObjectNode();
            graph.set("nodes", nodes);
            graph.set("edges", edges);
            return graph;
        }
    }
}
