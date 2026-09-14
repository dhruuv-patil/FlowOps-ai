package com.flowops.workflow.templates;

import com.flowops.api.TemplateDetailResponse;
import com.flowops.api.TemplateEnvelopes;
import com.flowops.api.TemplateSummaryResponse;
import com.flowops.api.WorkflowDetailResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.Workflow;
import com.flowops.repository.WorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the built-in {@link TemplateCatalog} and turns a chosen template into a
 * real draft workflow.
 *
 * <p>The catalogue is global product content, so listing needs no tenant scoping.
 * The workflow it produces is ordinary tenant data: its organization and creator
 * come from the principal, and it lands as a {@code DRAFT} exactly like a
 * hand-built one — the user still reviews, validates, publishes, and runs it
 * themselves. Nothing here executes a workflow.
 */
@Service
public class TemplateService {

    private final TemplateCatalog catalog;
    private final WorkflowRepository workflows;

    public TemplateService(TemplateCatalog catalog, WorkflowRepository workflows) {
        this.catalog = catalog;
        this.workflows = workflows;
    }

    public TemplateEnvelopes.Templates list() {
        List<TemplateSummaryResponse> rows =
                catalog.all().stream().map(TemplateSummaryResponse::of).toList();
        return new TemplateEnvelopes.Templates(rows);
    }

    public TemplateDetailResponse get(String slug) {
        return TemplateDetailResponse.of(require(slug));
    }

    /**
     * Creates a draft workflow in the caller's organization from the template's
     * graph. The graph is deep-copied so the shared catalogue instance can never
     * be mutated through the persisted entity.
     */
    @Transactional
    public WorkflowDetailResponse use(
            FlowOpsPrincipal principal, String slug, UseTemplateRequest request) {
        WorkflowTemplate template = require(slug);
        String name = request == null || request.name() == null ? template.name() : request.name();
        Workflow workflow = Workflow.create(
                principal.organizationId(),
                principal.userId(),
                name,
                template.description(),
                template.graph().deepCopy());
        workflows.save(workflow);
        return WorkflowDetailResponse.of(workflow);
    }

    private WorkflowTemplate require(String slug) {
        return catalog.find(slug).orElseThrow(() -> new ApiException(ErrorCode.TEMPLATE_NOT_FOUND));
    }
}
