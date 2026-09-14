package com.flowops.workflow.templates;

import com.flowops.api.TemplateDetailResponse;
import com.flowops.api.TemplateEnvelopes;
import com.flowops.api.WorkflowDetailResponse;
import com.flowops.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The built-in template gallery (M5 slice 5).
 *
 * <p>Listing and previewing are catalogue reads, but they still require a signed-in
 * member: everything under {@code /api} except the explicitly public paths in
 * {@code SecurityConfig} is authenticated, and calling
 * {@link AuthenticatedUser#require()} keeps that explicit here too. Creating from a
 * template writes into the caller's own organization only.
 */
@RestController
@RequestMapping("/api/templates")
@Tag(name = "Templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "List the built-in workflow templates")
    public TemplateEnvelopes.Templates list() {
        AuthenticatedUser.require();
        return templateService.list();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Preview one template, including its graph")
    public TemplateDetailResponse get(@PathVariable String slug) {
        AuthenticatedUser.require();
        return templateService.get(slug);
    }

    @PostMapping("/{slug}/use")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an editable draft workflow from a template")
    public WorkflowDetailResponse use(
            @PathVariable String slug,
            @Valid @RequestBody(required = false) UseTemplateRequest request) {
        return templateService.use(AuthenticatedUser.require(), slug, request);
    }
}
