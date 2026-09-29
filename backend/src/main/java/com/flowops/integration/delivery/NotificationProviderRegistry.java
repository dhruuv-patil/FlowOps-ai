package com.flowops.integration.delivery;

import com.flowops.integration.provider.IntegrationRegistry;
import com.flowops.integration.provider.IntegrationType;
import java.util.List;
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

    private final IntegrationRegistry registry;

    public NotificationProviderRegistry(IntegrationRegistry registry) {
        this.registry = registry;
    }

    /** True when a delivery provider is registered for the type. */
    public boolean supports(IntegrationType type) {
        if (type == null) return false;
        try {
            return registry.get(type) instanceof NotificationProvider;
        } catch (Exception e) {
            return false;
        }
    }

    /** True when a delivery provider is registered for the wire string. */
    public boolean supportsWire(String wire) {
        return supports(IntegrationType.fromWire(wire));
    }

    /** Returns the delivery provider for the type. */
    public NotificationProvider get(IntegrationType type) {
        return registry.get(type, NotificationProvider.class);
    }

    /** Returns the provider for a wire string (e.g. "slack"). */
    public NotificationProvider getByWire(String wire) {
        IntegrationType type = IntegrationType.fromWire(wire);
        return get(type);
    }

    /** All registered delivery providers (notifications section of the catalog). */
    public List<NotificationProvider> all() {
        return registry.all().stream()
                .filter(p -> p instanceof NotificationProvider)
                .map(p -> (NotificationProvider) p)
                .collect(Collectors.toList());
    }
}
