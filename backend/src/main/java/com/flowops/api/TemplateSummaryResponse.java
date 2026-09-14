package com.flowops.api;

import com.flowops.workflow.templates.WorkflowTemplate;
import java.util.List;

/**
 * Gallery row for a built-in template. The graph is omitted from the listing and
 * carried only by {@link TemplateDetailResponse}, so the gallery stays light.
 */
public record TemplateSummaryResponse(
        String slug,
        String name,
        String description,
        String category,
        String icon,
        List<String> tags,
        int nodeCount) {

    public static TemplateSummaryResponse of(WorkflowTemplate template) {
        return new TemplateSummaryResponse(
                template.slug(),
                template.name(),
                template.description(),
                template.category(),
                template.icon(),
                template.tags(),
                template.nodeCount());
    }
}
