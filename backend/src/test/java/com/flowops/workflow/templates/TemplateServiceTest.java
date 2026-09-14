package com.flowops.workflow.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.api.WorkflowDetailResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.Role;
import com.flowops.domain.Workflow;
import com.flowops.domain.WorkflowStatus;
import com.flowops.repository.WorkflowRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Using a template must produce ordinary tenant data: the caller's organization,
 * a DRAFT, and a graph that is a copy rather than the shared catalogue instance.
 */
class TemplateServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TemplateCatalog catalog = new TemplateCatalog(mapper);
    private final WorkflowRepository workflows = mock(WorkflowRepository.class);
    private final TemplateService service = new TemplateService(catalog, workflows);

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final FlowOpsPrincipal principal = new FlowOpsPrincipal(
            userId, UUID.randomUUID(), orgId, Role.MEMBER, "member@example.com");

    @Test
    void useCreatesADraftInTheCallersOrganizationWithACopyOfTheTemplateGraph() {
        WorkflowTemplate template = catalog.find("api-health-check").orElseThrow();

        WorkflowDetailResponse response = service.use(principal, "api-health-check", null);

        ArgumentCaptor<Workflow> saved = ArgumentCaptor.forClass(Workflow.class);
        verify(workflows).save(saved.capture());
        Workflow workflow = saved.getValue();
        assertThat(workflow.getOrganizationId()).isEqualTo(orgId);
        assertThat(workflow.getCreatedBy()).isEqualTo(userId);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.DRAFT);
        assertThat(workflow.getLatestVersion()).isNull();
        assertThat(workflow.getName()).isEqualTo(template.name());
        assertThat(workflow.getDraftGraph()).isEqualTo(template.graph());
        // A copy, not the shared catalogue node: mutating one must not touch the other.
        assertThat(workflow.getDraftGraph()).isNotSameAs(template.graph());
        assertThat(response.id()).isEqualTo(workflow.getId());
    }

    @Test
    void aSuppliedNameOverridesTheTemplatesOwn() {
        service.use(principal, "webhook-ai-summary", new UseTemplateRequest("  My summarizer  "));

        ArgumentCaptor<Workflow> saved = ArgumentCaptor.forClass(Workflow.class);
        verify(workflows).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("My summarizer");
    }

    @Test
    void anUnknownSlugIsTemplateNotFoundAndWritesNothing() {
        assertThatThrownBy(() -> service.use(principal, "no-such-template", null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.TEMPLATE_NOT_FOUND);
        verify(workflows, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
