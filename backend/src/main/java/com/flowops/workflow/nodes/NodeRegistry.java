package com.flowops.workflow.nodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The single source of truth for which node types exist and how they are shaped.
 *
 * <p>Kept deliberately data-driven: the builder palette, the config panel, and
 * the graph validator all read from the same definitions, so a node added here
 * shows up everywhere consistently. Executors for these types arrive in M3; a
 * type appearing here is a promise that M3 will run it, never fake output.
 */
@Component
public class NodeRegistry {

    private final Map<String, NodeDefinition> definitions = new LinkedHashMap<>();

    public NodeRegistry() {
        register(new NodeDefinition(
                "manual_trigger", "Manual Trigger",
                "Starts the workflow when a user clicks Run.",
                NodeCategory.TRIGGER, "MousePointerClick", true, 0, List.of("out"),
                List.of()));

        register(new NodeDefinition(
                "webhook_trigger", "Webhook Trigger",
                "Starts the workflow when an HTTP request hits its webhook URL.",
                NodeCategory.TRIGGER, "Webhook", true, 0, List.of("out"),
                List.of(
                        ConfigField.select("method", "HTTP Method", false,
                                List.of("POST", "GET", "PUT")))));

        register(new NodeDefinition(
                "http_request", "HTTP Request",
                "Calls an external HTTP endpoint and captures the response.",
                NodeCategory.ACTION, "Globe", false, 1, List.of("out"),
                List.of(
                        ConfigField.select("method", "Method", true,
                                List.of("GET", "POST", "PUT", "PATCH", "DELETE")),
                        ConfigField.text("url", "URL", true,
                                "Supports {{variable}} interpolation."),
                        ConfigField.json("headers", "Headers", false,
                                "JSON object of header name/value pairs."),
                        ConfigField.multiline("body", "Body", false,
                                "Request body; supports {{variable}} interpolation."))));

        register(new NodeDefinition(
                "condition", "Condition",
                "Branches execution on a boolean expression.",
                NodeCategory.LOGIC, "GitBranch", false, 1, List.of("true", "false"),
                List.of(
                        ConfigField.text("expression", "Expression", true,
                                "e.g. {{http_request.status}} == 200"))));

        register(new NodeDefinition(
                "transform", "Transform",
                "Reshapes data into new variables for downstream nodes.",
                NodeCategory.LOGIC, "Shuffle", false, 1, List.of("out"),
                List.of(
                        ConfigField.json("mapping", "Mapping", true,
                                "JSON object mapping output keys to {{variable}} expressions."))));

        register(new NodeDefinition(
                "delay", "Delay",
                "Pauses the workflow for a fixed duration.",
                NodeCategory.ACTION, "Clock", false, 1, List.of("out"),
                List.of(
                        ConfigField.number("seconds", "Delay (seconds)", true,
                                "How long to wait before continuing."))));

        register(new NodeDefinition(
                "notification", "Notification",
                "Sends a message through a configured channel.",
                NodeCategory.ACTION, "Bell", false, 1, List.of("out"),
                List.of(
                        ConfigField.select("channel", "Channel", true,
                                List.of("email", "slack", "webhook")),
                        ConfigField.multiline("message", "Message", true,
                                "Supports {{variable}} interpolation."))));

        register(new NodeDefinition(
                "human_approval", "Human Approval",
                "Pauses until a human approves or rejects, then branches.",
                NodeCategory.LOGIC, "UserCheck", false, 1, List.of("approved", "rejected"),
                List.of(
                        ConfigField.multiline("prompt", "Approval prompt", true,
                                "Shown to the approver."))));

        register(new NodeDefinition(
                "ai_agent", "AI Agent",
                "Runs an AI agent with instructions and returns structured output.",
                NodeCategory.AI, "Bot", false, 1, List.of("out"),
                List.of(
                        ConfigField.multiline("instructions", "Instructions", true,
                                "System instructions for the agent."),
                        ConfigField.multiline("input", "Input", false,
                                "User input; supports {{variable}} interpolation."))));
    }

    private void register(NodeDefinition definition) {
        definitions.put(definition.type(), definition);
    }

    /** All definitions in a stable, palette-friendly order. */
    public List<NodeDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public Optional<NodeDefinition> find(String type) {
        return Optional.ofNullable(definitions.get(type));
    }

    public boolean isKnown(String type) {
        return definitions.containsKey(type);
    }
}
