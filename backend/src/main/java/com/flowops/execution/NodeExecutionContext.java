package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.config.ExecutionProperties;
import com.flowops.workflow.graph.GraphNode;
import java.util.Map;

/**
 * Everything a {@link NodeExecutor} is handed for one node run: the graph node and
 * its config, the resolved variable root for {@code {{...}}} interpolation, shared
 * helpers, the engine's tuning limits, and a logger.
 *
 * <p>Executors read from here and return a {@link NodeResult}; they never touch the
 * database, the SSE stream, or other nodes directly. That keeps each executor a
 * pure function of (config, variables) — easy to reason about and impossible to use
 * as a lateral channel between tenants.
 */
public final class NodeExecutionContext {

    private final GraphNode node;
    private final JsonNode variables;
    private final VariableInterpolator interpolator;
    private final ObjectMapper mapper;
    private final ExecutionProperties properties;
    private final NodeLogger logger;

    public NodeExecutionContext(
            GraphNode node,
            JsonNode variables,
            VariableInterpolator interpolator,
            ObjectMapper mapper,
            ExecutionProperties properties,
            NodeLogger logger) {
        this.node = node;
        this.variables = variables;
        this.interpolator = interpolator;
        this.mapper = mapper;
        this.properties = properties;
        this.logger = logger;
    }

    public GraphNode node() {
        return node;
    }

    public Map<String, Object> config() {
        return node.config();
    }

    /** The variable root: {@code trigger} plus every succeeded node's output. */
    public JsonNode variables() {
        return variables;
    }

    /** Replaces {@code {{ path }}} tokens in {@code template} with resolved values. */
    public String interpolate(String template) {
        return interpolator.interpolate(template, variables);
    }

    /** Resolves a single dotted path to a JSON node (missing if absent). */
    public JsonNode resolve(String path) {
        return interpolator.resolve(variables, path);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public ExecutionProperties properties() {
        return properties;
    }

    public NodeLogger log() {
        return logger;
    }

    /** Reads a config value as a trimmed string, interpolating it; null-safe. */
    public String configString(String key) {
        Object raw = config().get(key);
        return raw == null ? null : interpolate(String.valueOf(raw));
    }
}
