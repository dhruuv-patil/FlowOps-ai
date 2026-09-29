package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.workflow.templates.WorkflowTemplate;
import java.util.List;

public record TemplateDetailResponse(
        String slug,
        String name,
        String description,
        String category,
        String icon,
        List<String> tags,
        int nodeCount,
        List<String> providers,
        JsonNode graph) {

    public TemplateDetailResponse {
        if (tags == null) {
            tags = List.of();
        } else {
            tags = List.copyOf(tags);
        }

        if (providers == null) {
            providers = List.of();
        } else {
            providers = List.copyOf(providers);
        }

        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
    }

    public static TemplateDetailResponse of(
            WorkflowTemplate template) {

        return new TemplateDetailResponse(
                template.slug(),
                template.name(),
                template.description(),
                template.category(),
                template.icon(),
                template.tags(),
                template.nodeCount(),
                template.providers(),
                template.graph());
    }
}