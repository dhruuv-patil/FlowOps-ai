package com.flowops.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of "Create with AI". {@code graph} is a builder-ready workflow graph
 * ({@code nodes}/{@code edges}) already sanitized against the node registry; the
 * frontend drops it straight onto the canvas. {@code notes} is optional model
 * commentary. The graph is never run automatically.
 */
public record GenerateWorkflowResponse(JsonNode graph, String notes) {
}
