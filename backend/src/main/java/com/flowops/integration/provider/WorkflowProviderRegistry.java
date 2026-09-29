package com.flowops.integration.provider;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Registry of available workflow providers.
 * Spring auto-registers any bean implementing {@link WorkflowProvider}.
 */
@Service
public class WorkflowProviderRegistry {

    private final IntegrationRegistry registry;

    public WorkflowProviderRegistry(IntegrationRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the provider for the given type.
     *
     * @throws ProviderConfigurationException if no provider is registered for the type
     */
    public WorkflowProvider get(IntegrationType type) {
        return registry.get(type, WorkflowProvider.class);
    }

    /** Returns all registered providers (for the catalog UI). */
    public List<WorkflowProvider> all() {
        return registry.all().stream()
                .filter(p -> p instanceof WorkflowProvider)
                .map(p -> (WorkflowProvider) p)
                .collect(Collectors.toList());
    }

    /** Returns the provider for the wire type string. */
    public WorkflowProvider getByWire(String wire) {
        IntegrationType type = IntegrationType.fromWire(wire);
        if (type == null) {
            throw new ProviderConfigurationException(null,
                    "Unknown provider type: " + wire);
        }
        return get(type);
    }
}
