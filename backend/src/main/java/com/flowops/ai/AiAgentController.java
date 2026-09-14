package com.flowops.ai;

import com.flowops.api.AgentRunResponse;
import com.flowops.api.AiAgentDetailResponse;
import com.flowops.api.AiAgentEnvelopes;
import com.flowops.domain.Role;
import com.flowops.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI agent library CRUD and the test console (M4).
 *
 * <p>The tenant is always the caller's current organization, read from the principal
 * by {@link AuthenticatedUser}. No endpoint accepts an organization id.
 */
@RestController
@RequestMapping("/api/ai/agents")
@Tag(name = "AI Agents")
public class AiAgentController {

    private final AiAgentService agentService;

    public AiAgentController(AiAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    @Operation(summary = "List AI agents in the current organization")
    public AiAgentEnvelopes.Agents list() {
        return agentService.list(AuthenticatedUser.require());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an AI agent")
    public AiAgentDetailResponse create(@Valid @RequestBody CreateAiAgentRequest request) {
        return agentService.create(AuthenticatedUser.requireRole(Role.MEMBER), request);
    }

    @GetMapping("/{agentId}")
    @Operation(summary = "Get an AI agent with its instructions")
    public AiAgentDetailResponse get(@PathVariable UUID agentId) {
        return agentService.get(AuthenticatedUser.require(), agentId);
    }

    @PatchMapping(path = "/{agentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an AI agent")
    public AiAgentDetailResponse update(
            @PathVariable UUID agentId, @Valid @RequestBody UpdateAiAgentRequest request) {
        return agentService.update(AuthenticatedUser.requireRole(Role.MEMBER), agentId, request);
    }

    @DeleteMapping("/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an AI agent")
    public ResponseEntity<Void> delete(@PathVariable UUID agentId) {
        agentService.delete(AuthenticatedUser.requireRole(Role.MEMBER), agentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{agentId}/run")
    @Operation(summary = "Run the agent against test input (test console)")
    public AgentRunResponse run(
            @PathVariable UUID agentId,
            @Valid @RequestBody(required = false) AgentTestRequest request) {
        return agentService.testRun(AuthenticatedUser.requireRole(Role.MEMBER), agentId, request);
    }
}
