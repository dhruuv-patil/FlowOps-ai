package com.flowops.workflow.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * A node in a workflow graph. {@code type} keys into the {@code NodeRegistry};
 * {@code config} holds the values captured by that type's config fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphNode(String id, String type, GraphPosition position, Map<String, Object> data) {

    public Map<String, Object> data() {
        return data == null ? Map.of() : data;
    }

    /**
     * The config sub-map. The frontend nests captured field values under
     * {@code data.config}; a missing map means "nothing configured yet".
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> config() {
        Object config = data().get("config");
        return config instanceof Map ? (Map<String, Object>) config : Map.of();
    }
}
