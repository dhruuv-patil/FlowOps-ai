package com.flowops.workflow.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.workflow.nodes.NodeRegistry;
import com.flowops.workflow.validation.Severity;
import com.flowops.workflow.validation.ValidationResult;
import com.flowops.workflow.validation.WorkflowValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The catalogue's contract: a template must be a graph the product itself accepts.
 *
 * <p>These run the real {@link WorkflowValidator} over every built-in template, so
 * if a node type gains a required field the catalogue misses, this fails rather
 * than shipping a template that lands broken in a user's builder.
 */
class TemplateCatalogTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TemplateCatalog catalog = new TemplateCatalog(mapper);
    private final WorkflowValidator validator = new WorkflowValidator(new NodeRegistry(), mapper);

    @Test
    void everyTemplateIsAGraphTheValidatorAcceptsWithoutWarnings() {
        assertThat(catalog.all()).isNotEmpty();
        for (WorkflowTemplate template : catalog.all()) {
            ValidationResult result = validator.validate(template.graph());
            assertThat(result.valid())
                    .as("%s must publish cleanly: %s", template.slug(), result.issues())
                    .isTrue();
            // A warning here would mean an unreachable node — a template should not
            // ship dead branches either.
            assertThat(result.issues())
                    .as("%s should have no warnings", template.slug())
                    .noneMatch(issue -> issue.severity() == Severity.WARNING);
        }
    }

    @Test
    void slugsAreUniqueAndMetadataIsPopulated() {
        List<String> slugs = catalog.all().stream().map(WorkflowTemplate::slug).toList();
        assertThat(slugs).doesNotHaveDuplicates();
        for (WorkflowTemplate template : catalog.all()) {
            assertThat(template.name()).isNotBlank();
            assertThat(template.description()).isNotBlank();
            assertThat(template.category()).isNotBlank();
            assertThat(template.icon()).isNotBlank();
            assertThat(template.tags()).isNotEmpty();
            assertThat(template.nodeCount()).isGreaterThan(1);
        }
    }

    @Test
    void lookupIsExactAndUnknownSlugsAreAbsentRatherThanGuessed() {
        assertThat(catalog.find("api-health-check")).isPresent();
        assertThat(catalog.find("  api-health-check  ")).isPresent();
        assertThat(catalog.find("api-health")).isEmpty();
        assertThat(catalog.find(null)).isEmpty();
    }

    @Test
    void everyNodeCarriesAPositionSoTheBuilderDoesNotStackThemAtTheOrigin() {
        for (WorkflowTemplate template : catalog.all()) {
            template.graph().get("nodes").forEach(node -> {
                assertThat(node.hasNonNull("position")).isTrue();
                assertThat(node.get("position").get("x").asInt()).isPositive();
                assertThat(node.get("data").get("label").asText()).isNotBlank();
            });
        }
    }
}
