package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowops.workflow.templates.WorkflowTemplate;
import java.util.List;

/**
 * A template with its graph, for the read-only preview shown before the user
 * decides to use it. Previewing never creates or runs anything.
 */
public record TemplateDetailResponse(
        String slug,
        String name,
        String description,
        String category,
        String icon,
        List<String> tags,
        int nodeCount,
        JsonNode graph) {

    public static TemplateDetailResponse of(WorkflowTemplate template) {
        return new TemplateDetailResponse(
                template.slug(),
                template.name(),
                template.description(),
                template.category(),
                template.icon(),
                template.tags(),
                template.nodeCount(),
                template.graph());
    }
}
