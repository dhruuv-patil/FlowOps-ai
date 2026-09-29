package com.flowops.integration.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;



/**
 * Unified registry of all integration providers.
 * Spring auto-registers any bean implementing {@link IntegrationProvider}.
 */
@Service
public class IntegrationRegistry {

    private final Map<IntegrationType, IntegrationProvider> providers;

    public IntegrationRegistry(List<IntegrationProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        IntegrationProvider::type,
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
    public IntegrationProvider get(IntegrationType type) {
        IntegrationProvider provider = providers.get(type);
        if (provider == null) {
            throw new ProviderConfigurationException(type,
                    "No provider registered for type: " + type.wire());
        }
        return provider;
    }

    /**
     * Returns the capability-safe provider for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T extends IntegrationProvider> T get(IntegrationType type, Class<T> clazz) {
        IntegrationProvider provider = get(type);
        if (!clazz.isInstance(provider)) {
            throw new ProviderConfigurationException(type,
                    "Provider " + type.wire() + " does not support " + clazz.getSimpleName());
        }
        return (T) provider;
    }

    /** Returns all registered providers (for the catalog UI). */
    public List<IntegrationProvider> all() {
    return providers.values().stream()
            .sorted(java.util.Comparator.comparing(
                    provider -> provider.type().wire()))
            .toList();
}

    /** Returns the provider for the wire type string. */
    public IntegrationProvider getByWire(String wire) {
        IntegrationType type = IntegrationType.fromWire(wire);
        if (type == null) {
            throw new ProviderConfigurationException(null,
                    "Unknown provider type: " + wire);
        }
        return get(type);
    }

   public IntegrationProvider require(String wire) {
    try {
        return getByWire(wire);
    } catch (ProviderConfigurationException e) {
        throw new ApiException(ErrorCode.INTEGRATION_NOT_FOUND);
    }
}
}
