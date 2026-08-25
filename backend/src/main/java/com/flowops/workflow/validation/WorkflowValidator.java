package com.flowops.workflow.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.workflow.graph.GraphDocument;
import com.flowops.workflow.graph.GraphEdge;
import com.flowops.workflow.graph.GraphNode;
import com.flowops.workflow.nodes.ConfigField;
import com.flowops.workflow.nodes.NodeDefinition;
import com.flowops.workflow.nodes.NodeRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Structural validation of a workflow graph. Pure and side-effect free: it never
 * touches the database, so it is reused unchanged by the live "Validate" button,
 * the publish gate, and (M3) the pre-execution check.
 *
 * <p>Errors block publishing; warnings do not. The rules enforce that a graph is
 * a well-formed, single-entry, acyclic pipeline of known, configured nodes.
 */
@Component
public class WorkflowValidator {

    private final NodeRegistry registry;
    private final ObjectMapper objectMapper;

    public WorkflowValidator(NodeRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public ValidationResult validate(JsonNode graphJson) {
        List<ValidationIssue> issues = new ArrayList<>();

        GraphDocument graph;
        try {
            graph = objectMapper.treeToValue(graphJson, GraphDocument.class);
        } catch (Exception e) {
            return ValidationResult.of(List.of(ValidationIssue.graphError(
                    "GRAPH_UNREADABLE", "The workflow graph is malformed and could not be parsed.")));
        }

        List<GraphNode> nodes = graph.nodes();
        List<GraphEdge> edges = graph.edges();

        if (nodes.isEmpty()) {
            issues.add(ValidationIssue.graphError(
                    "EMPTY_GRAPH", "Add at least one trigger node to build a workflow."));
            return ValidationResult.of(issues);
        }

        // Index nodes by id and detect duplicates.
        Map<String, GraphNode> nodesById = new HashMap<>();
        for (GraphNode node : nodes) {
            if (node.id() == null || node.id().isBlank()) {
                issues.add(ValidationIssue.graphError("NODE_MISSING_ID", "A node is missing its id."));
                continue;
            }
            if (nodesById.putIfAbsent(node.id(), node) != null) {
                issues.add(ValidationIssue.nodeError(
                        "DUPLICATE_NODE_ID", "Two nodes share the id \"" + node.id() + "\".", node.id()));
            }
        }

        validateNodes(nodes, issues);
        int triggerCount = countTriggers(nodes);
        if (triggerCount == 0) {
            issues.add(ValidationIssue.graphError(
                    "NO_TRIGGER", "A workflow needs exactly one trigger node to start it."));
        } else if (triggerCount > 1) {
            issues.add(ValidationIssue.graphError(
                    "MULTIPLE_TRIGGERS", "A workflow can only have one trigger node."));
        }

        validateEdges(edges, nodesById, issues);
        detectOrphans(nodes, edges, issues);
        detectCycle(nodesById.keySet(), edges, issues);

        return ValidationResult.of(issues);
    }

    private void validateNodes(List<GraphNode> nodes, List<ValidationIssue> issues) {
        for (GraphNode node : nodes) {
            if (node.id() == null) {
                continue;
            }
            NodeDefinition def = registry.find(node.type()).orElse(null);
            if (def == null) {
                issues.add(ValidationIssue.nodeError(
                        "UNKNOWN_NODE_TYPE",
                        "Unknown node type \"" + node.type() + "\".", node.id()));
                continue;
            }
            Map<String, Object> config = node.config();
            for (ConfigField field : def.configFields()) {
                if (!field.required()) {
                    continue;
                }
                Object value = config.get(field.key());
                if (value == null || (value instanceof String s && s.isBlank())) {
                    issues.add(ValidationIssue.nodeError(
                            "MISSING_REQUIRED_CONFIG",
                            def.label() + " is missing required field \"" + field.label() + "\".",
                            node.id()));
                }
            }
        }
    }

    private int countTriggers(List<GraphNode> nodes) {
        return (int) nodes.stream()
                .map(n -> registry.find(n.type()).orElse(null))
                .filter(d -> d != null && d.trigger())
                .count();
    }

    private void validateEdges(
            List<GraphEdge> edges, Map<String, GraphNode> nodesById, List<ValidationIssue> issues) {
        for (GraphEdge edge : edges) {
            if (edge.source() == null || edge.target() == null) {
                issues.add(ValidationIssue.edgeError(
                        "EDGE_INCOMPLETE", "A connection is missing an endpoint.", edge.id()));
                continue;
            }
            if (!nodesById.containsKey(edge.source()) || !nodesById.containsKey(edge.target())) {
                issues.add(ValidationIssue.edgeError(
                        "EDGE_DANGLING", "A connection references a node that no longer exists.", edge.id()));
                continue;
            }
            GraphNode target = nodesById.get(edge.target());
            registry.find(target.type()).ifPresent(def -> {
                if (def.trigger()) {
                    issues.add(ValidationIssue.nodeError(
                            "TRIGGER_HAS_INPUT",
                            def.label() + " is a trigger and cannot have an incoming connection.",
                            target.id()));
                }
            });
        }
    }

    /** Non-trigger nodes with no inbound edge are unreachable — a warning, not an error. */
    private void detectOrphans(
            List<GraphNode> nodes, List<GraphEdge> edges, List<ValidationIssue> issues) {
        Set<String> withInbound = new HashSet<>();
        for (GraphEdge edge : edges) {
            if (edge.target() != null) {
                withInbound.add(edge.target());
            }
        }
        for (GraphNode node : nodes) {
            if (node.id() == null) {
                continue;
            }
            boolean isTrigger = registry.find(node.type()).map(NodeDefinition::trigger).orElse(false);
            if (!isTrigger && !withInbound.contains(node.id())) {
                issues.add(ValidationIssue.nodeWarning(
                        "UNREACHABLE_NODE",
                        "This node has no incoming connection and will never run.", node.id()));
            }
        }
    }

    /** Kahn's algorithm: if any node remains after removing all zero-indegree nodes, there is a cycle. */
    private void detectCycle(Set<String> nodeIds, List<GraphEdge> edges, List<ValidationIssue> issues) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (String id : nodeIds) {
            indegree.put(id, 0);
            adjacency.put(id, new ArrayList<>());
        }
        for (GraphEdge edge : edges) {
            if (edge.source() == null || edge.target() == null) {
                continue;
            }
            if (!indegree.containsKey(edge.source()) || !indegree.containsKey(edge.target())) {
                continue;
            }
            adjacency.get(edge.source()).add(edge.target());
            indegree.merge(edge.target(), 1, Integer::sum);
        }
        Deque<String> queue = new ArrayDeque<>();
        indegree.forEach((id, degree) -> {
            if (degree == 0) {
                queue.add(id);
            }
        });
        int visited = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            visited++;
            for (String next : adjacency.get(current)) {
                if (indegree.merge(next, -1, Integer::sum) == 0) {
                    queue.add(next);
                }
            }
        }
        if (visited < nodeIds.size()) {
            issues.add(ValidationIssue.graphError(
                    "CYCLE_DETECTED", "The workflow contains a loop; connections must not form a cycle."));
        }
    }
}
