package com.flowops.integration.delivery;

import com.flowops.integration.provider.IntegrationType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Registry of available delivery (notification) providers. Spring auto-registers any
 * bean implementing {@link NotificationProvider}, mirroring
 * {@code WorkflowProviderRegistry} — the two registries together form the
 * provider catalog served by the integrations API.
 */
@Service
public class NotificationProviderRegistry {

    private final Map<IntegrationType, NotificationProvider> providers;

    public NotificationProviderRegistry(List<NotificationProvider> providerList) {
        this.providers = providerList.stream().collect(Collectors.toMap(
                NotificationProvider::type,
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException(
                            "Duplicate delivery provider for type " + a.type());
                }));
    }

    /** True when a delivery provider is registered for the type. */
    public boolean supports(IntegrationType type) {
        return type != null && providers.containsKey(type);
    }

    /** True when a delivery provider is registered for the wire string. */
    public boolean supportsWire(String wire) {
        return supports(IntegrationType.fromWire(wire));
    }

    /** Returns the delivery provider for the type. */
    public NotificationProvider get(IntegrationType type) {
        NotificationProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "No delivery provider registered for type: "
                            + (type == null ? "null" : type.wire()));
        }
        return provider;
    }

    /** Returns the provider for a wire string (e.g. "slack"). */
    public NotificationProvider getByWire(String wire) {
        IntegrationType type = IntegrationType.fromWire(wire);
        if (type == null || !providers.containsKey(type)) {
            throw new IllegalArgumentException("Unknown delivery provider: " + wire);
        }
        return get(type);
    }

    /** All registered delivery providers (notifications section of the catalog). */
    public List<NotificationProvider> all() {
        return List.copyOf(providers.values());
    }
}