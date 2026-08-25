package com.flowops.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Body for saving the working graph. The graph is stored verbatim as jsonb; it is
 * not required to be valid (you can save an in-progress, incomplete graph), only
 * well-formed JSON with the expected top-level shape.
 */
public record SaveGraphRequest(@NotNull JsonNode graph) {
}
