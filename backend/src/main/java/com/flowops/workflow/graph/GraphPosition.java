package com.flowops.workflow.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Canvas coordinates for a node. Not semantically meaningful; carried through verbatim. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphPosition(double x, double y) {
}
