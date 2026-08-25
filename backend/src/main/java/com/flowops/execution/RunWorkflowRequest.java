package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Body for starting a run. {@code input} is the trigger payload the run begins with,
 * readable downstream as {@code {{trigger.*}}}; it is optional (an empty object when
 * omitted), so a manual run needs no body at all.
 */
public record RunWorkflowRequest(JsonNode input) {
}
