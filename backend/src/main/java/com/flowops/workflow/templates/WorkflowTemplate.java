package com.flowops.workflow.templates;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Immutable built-in workflow template definition.
 *
 * <p>The template catalogue is global product content. A template contains
 * presentation metadata, the providers used by its workflow, and the actual
 * workflow graph that is copied into a tenant workflow when the template is used.
 */
public record WorkflowTemplate(
        String slug,
        String name,
        String description,
        String category,
        String icon,
        List<String> tags,
        List<String> providers,
        JsonNode graph) {

    public WorkflowTemplate {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Template slug must not be blank.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name must not be blank.");
        }

        if (description == null) {
            description = "";
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Template category must not be blank.");
        }

        if (icon == null || icon.isBlank()) {
            icon = "Workflow";
        }

        if (tags == null) {
            tags = List.of();
        } else {
            tags = List.copyOf(tags);
        }

        if (providers == null) {
            providers = List.of();
        } else {
            providers = providers.stream()
                    .filter(provider -> provider != null && !provider.isBlank())
                    .map(String::strip)
                    .distinct()
                    .toList();
        }

        if (graph == null || graph.isNull()) {
            throw new IllegalArgumentException("Template graph must not be null.");
        }
    }

    /**
     * Number of nodes in the template graph.
     */
    public int nodeCount() {
        JsonNode nodes = graph.path("nodes");

        return nodes.isArray()
                ? nodes.size()
                : 0;
    }
}