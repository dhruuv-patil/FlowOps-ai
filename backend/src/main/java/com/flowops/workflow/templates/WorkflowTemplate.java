package com.flowops.workflow.templates;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * A curated starting point for a workflow: real metadata plus a real graph made
 * only of registry node types, with every required field pre-filled.
 *
 * <p>Templates are product content, not tenant data — they live in code rather
 * than the database, so there is no per-organization scoping to get wrong and no
 * migration to run. "Using" one creates a genuine draft workflow in the caller's
 * own organization ({@link TemplateService#use}); nothing is ever executed.
 *
 * @param slug     stable url-safe id ({@code api-health-check})
 * @param name     human label
 * @param description one-line summary shown on the gallery card
 * @param category grouping label for the gallery
 * @param icon     PascalCase lucide icon name, resolved client-side
 * @param tags     short keywords, also used by the command palette
 * @param graph    the {@code {nodes, edges}} document copied into the new draft
 */
public record WorkflowTemplate(
        String slug,
        String name,
        String description,
        String category,
        String icon,
        List<String> tags,
        JsonNode graph) {

    /** Node count, for the gallery card. Cheap enough to derive on demand. */
    public int nodeCount() {
        JsonNode nodes = graph == null ? null : graph.get("nodes");
        return nodes == null ? 0 : nodes.size();
    }
}
