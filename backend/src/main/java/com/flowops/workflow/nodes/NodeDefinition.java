package com.flowops.workflow.nodes;

import java.util.List;

/**
 * The static description of a node type: its identity, palette metadata, port
 * shape, and config fields. Serialized to the frontend as the node registry
 * ({@code GET /api/node-types}) and consulted by the validator.
 *
 * @param type        stable string key, e.g. {@code http_request}
 * @param label       display name
 * @param description one-line summary for the palette and config header
 * @param category    palette grouping
 * @param icon        lucide icon name the frontend maps to a component
 * @param trigger     true for entry-point nodes (no inbound edges, start a run)
 * @param maxInputs   max inbound edges (0 for triggers)
 * @param outputs     named output ports; a plain node has {@code ["out"]}, a
 *                    branch has {@code ["true","false"]}
 * @param configFields the config panel schema
 */
public record NodeDefinition(
        String type,
        String label,
        String description,
        NodeCategory category,
        String icon,
        boolean trigger,
        int maxInputs,
        List<String> outputs,
        List<ConfigField> configFields) {
}
