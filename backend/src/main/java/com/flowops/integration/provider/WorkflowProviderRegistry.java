package com.flowops.integration.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Registry of available workflow providers.
 * Spring auto-registers any bean implementing {@link WorkflowProvider}.
 */
@Service
public class WorkflowProviderRegistry {

    private final Map<IntegrationType, WorkflowProvider> providers;

    public WorkflowProviderRegistry(List<WorkflowProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        WorkflowProvider::type,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate provider for type " + a.type());
                        }));
    }

    /**
     * Returns the provider for the given type.
     *
     * @throws ProviderConfigurationException if no provider is registered for the type
     */
    public WorkflowProvider get(IntegrationType type) {
        WorkflowProvider provider = providers.get(type);
        if (provider == null) {
            throw new ProviderConfigurationException(type,
                    "No provider registered for type: " + type.wire());
        }
        return provider;
    }

    /** Returns all registered providers (for the catalog UI). */
    public List<WorkflowProvider> all() {
        return List.copyOf(providers.values());
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