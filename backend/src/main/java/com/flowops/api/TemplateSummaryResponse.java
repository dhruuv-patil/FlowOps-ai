package com.flowops.api;

import com.flowops.workflow.templates.WorkflowTemplate;
import java.util.List;

/**
 * Lightweight template representation used by the template gallery.
 *
 * <p>Unlike {@link TemplateDetailResponse}, this response does not expose the
 * workflow graph. It includes the provider identifiers extracted from the real
 * template graph so the frontend can render the correct integration logos.
 */
public record TemplateSummaryResponse(
        String slug,
        String name,
        String description,
        String category,
        String icon,
        List<String> tags,
        int nodeCount,
        List<String> providers) {

    public TemplateSummaryResponse {
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
    }

    public static TemplateSummaryResponse of(
            WorkflowTemplate template) {

        return new TemplateSummaryResponse(
                template.slug(),
                template.name(),
                template.description(),
                template.category(),
                template.icon(),
                template.tags(),
                template.nodeCount(),
                template.providers());
    }
}