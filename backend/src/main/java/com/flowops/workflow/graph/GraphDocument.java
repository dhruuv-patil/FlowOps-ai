package com.flowops.workflow.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * The parsed shape of a workflow graph document. Deserialized from the stored
 * jsonb for validation and (in M3) execution. Unknown properties are ignored so
 * the frontend can carry extra view-only fields (selection state, colors) that
 * the backend does not care about.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphDocument(List<GraphNode> nodes, List<GraphEdge> edges) {

    public List<GraphNode> nodes() {
        return nodes == null ? List.of() : nodes;
    }

    public List<GraphEdge> edges() {
        return edges == null ? List.of() : edges;
    }
}
