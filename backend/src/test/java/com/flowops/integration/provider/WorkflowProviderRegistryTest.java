package com.flowops.integration.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowProviderRegistryTest {

    @Test
    void registryBuildsFromProviders() {
        WorkflowProvider provider = new TestProvider(IntegrationType.N8N);

        WorkflowProviderRegistry registry =
                new WorkflowProviderRegistry(List.of(provider));

        assertThat(registry.get(IntegrationType.N8N))
                .isSameAs(provider);

        assertThat(registry.all())
                .containsExactly(provider);
    }

    @Test
    void registryThrowsOnDuplicateType() {
        WorkflowProvider first = new TestProvider(IntegrationType.N8N);
        WorkflowProvider second = new TestProvider(IntegrationType.N8N);

        assertThatThrownBy(() ->
                new WorkflowProviderRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate provider for type N8N");
    }

    @Test
    void registryThrowsOnUnknownType() {
        WorkflowProviderRegistry registry =
                new WorkflowProviderRegistry(List.of(
                        new TestProvider(IntegrationType.N8N)));

        assertThatThrownBy(() ->
                registry.get(IntegrationType.SLACK))
                .isInstanceOf(ProviderConfigurationException.class)
                .hasMessageContaining("No provider registered for type");
    }

    @Test
    void registryThrowsOnUnknownWire() {
        WorkflowProviderRegistry registry =
                new WorkflowProviderRegistry(List.of(
                        new TestProvider(IntegrationType.N8N)));

        assertThatThrownBy(() ->
                registry.getByWire("unknown"))
                .isInstanceOf(ProviderConfigurationException.class)
                .hasMessageContaining("Unknown provider type: unknown");
    }

    private static record TestProvider(IntegrationType type)
            implements WorkflowProvider {

        @Override
        public String displayName() {
            return "Test Provider";
        }

        @Override
        public String description() {
            return "Test provider";
        }

        @Override
        public List<CredentialField> credentialFields() {
            return List.of();
        }

        @Override
        public ProviderCapabilities capabilities() {
            return new ProviderCapabilities(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        @Override
        public ConnectionTestResult testConnection(
                IntegrationContext context) {
            return null;
        }

        @Override
        public List<ExternalWorkflow> discoverWorkflows(
                IntegrationContext context) {
            return List.of();
        }

        @Override
        public ExecutionPage<ExternalExecution> fetchExecutions(
                IntegrationContext context,
                SyncCursor cursor) {
            return new ExecutionPage<>(
                    List.of(),
                    SyncCursor.EMPTY,
                    false
            );
        }

        @Override
        public List<ExternalNodeExecution> fetchNodeExecutions(
                IntegrationContext context,
                ExternalExecution execution) {
            return List.of();
        }
    }
}