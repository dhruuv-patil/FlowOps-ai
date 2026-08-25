package com.flowops.workflow;

import com.flowops.api.WorkflowEnvelopes;
import com.flowops.security.AuthenticatedUser;
import com.flowops.workflow.nodes.NodeRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The node registry the builder renders its palette and config panels from. Read
 * requires authentication but is not org-scoped: node types are global.
 */
@RestController
@RequestMapping("/api/node-types")
@Tag(name = "Node Types")
public class NodeTypeController {

    private final NodeRegistry registry;

    public NodeTypeController(NodeRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    @Operation(summary = "List all available workflow node types")
    public WorkflowEnvelopes.NodeTypes list() {
        AuthenticatedUser.require();
        return new WorkflowEnvelopes.NodeTypes(registry.all());
    }
}
