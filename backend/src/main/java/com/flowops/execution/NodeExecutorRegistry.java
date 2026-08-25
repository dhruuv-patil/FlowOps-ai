package com.flowops.execution;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Indexes every {@link NodeExecutor} bean by its {@link NodeExecutor#type()}, so
 * the engine can dispatch a graph node to its executor in O(1). Spring injects all
 * executors; a duplicate type is a wiring bug and fails fast at startup.
 */
@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> byType;

    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        this.byType = executors.stream()
                .collect(Collectors.toUnmodifiableMap(NodeExecutor::type, Function.identity()));
    }

    public Optional<NodeExecutor> find(String type) {
        return Optional.ofNullable(byType.get(type));
    }
}
