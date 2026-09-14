package com.flowops.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flowops.api.AgentRunResponse;
import com.flowops.api.AiAgentDetailResponse;
import com.flowops.api.AiAgentEnvelopes;
import com.flowops.api.AiAgentSummaryResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.AiAgent;
import com.flowops.repository.AiAgentRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI agent library: org-scoped CRUD plus a test console that runs an agent through
 * the AI service.
 *
 * <p>Every read and write is scoped to the caller's organization (from the principal,
 * never the request body). An agent belonging to another tenant is indistinguishable
 * from one that does not exist — both yield {@link ErrorCode#AI_AGENT_NOT_FOUND}
 * (contract §2, anti-enumeration). The provider key never enters this class; the AI
 * service reports {@code configured:false} and we surface that honestly.
 */
@Service
public class AiAgentService {

    private final AiAgentRepository agents;
    private final AiServiceClient aiService;
    private final ObjectMapper objectMapper;

    public AiAgentService(
            AiAgentRepository agents, AiServiceClient aiService, ObjectMapper objectMapper) {
        this.agents = agents;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AiAgentEnvelopes.Agents list(FlowOpsPrincipal principal) {
        List<AiAgentSummaryResponse> rows = agents
                .findByOrganizationIdOrderByUpdatedAtDesc(principal.organizationId())
                .stream()
                .map(agent -> AiAgentSummaryResponse.of(agent, toolList(agent.getTools())))
                .toList();
        return new AiAgentEnvelopes.Agents(rows);
    }

    @Transactional
    public AiAgentDetailResponse create(FlowOpsPrincipal principal, CreateAiAgentRequest request) {
        AiAgent agent = AiAgent.create(
                principal.organizationId(),
                principal.userId(),
                request.name(),
                request.description(),
                request.instructions(),
                request.model(),
                toolsJson(request.tools()));
        agents.save(agent);
        return AiAgentDetailResponse.of(agent, request.tools());
    }

    @Transactional(readOnly = true)
    public AiAgentDetailResponse get(FlowOpsPrincipal principal, UUID agentId) {
        AiAgent agent = require(principal, agentId);
        return AiAgentDetailResponse.of(agent, toolList(agent.getTools()));
    }

    @Transactional
    public AiAgentDetailResponse update(
            FlowOpsPrincipal principal, UUID agentId, UpdateAiAgentRequest request) {
        AiAgent agent = require(principal, agentId);
        agent.editDetails(
                request.name(),
                request.description(),
                request.instructions(),
                request.model(),
                toolsJson(request.tools()));
        return AiAgentDetailResponse.of(agent, request.tools());
    }

    @Transactional
    public void delete(FlowOpsPrincipal principal, UUID agentId) {
        AiAgent agent = require(principal, agentId);
        agents.delete(agent);
    }

    /**
     * Runs the agent against optional test input via the AI service. When the service
     * has no key the response is {@code configured:false} with no output — an honest
     * "not configured", never a fabricated completion.
     */
    @Transactional(readOnly = true)
    public AgentRunResponse testRun(FlowOpsPrincipal principal, UUID agentId, AgentTestRequest request) {
        AiAgent agent = require(principal, agentId);
        String input = request == null ? null : request.input();
        AiServiceClient.AgentRun run = aiService.runAgent(
                agent.getInstructions(),
                agent.getModel(),
                toolList(agent.getTools()),
                input == null ? "" : input);
        return AgentRunResponse.of(run);
    }

    /** Fetch scoped to the caller's org, or 404. The single choke point for tenant isolation. */
    private AiAgent require(FlowOpsPrincipal principal, UUID agentId) {
        return agents
                .findByIdAndOrganizationId(agentId, principal.organizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.AI_AGENT_NOT_FOUND));
    }

    /** Stored tools column (a JSON string array) → a plain list of names. */
    private List<String> toolList(JsonNode tools) {
        if (tools == null || !tools.isArray()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (JsonNode tool : tools) {
            if (tool != null && tool.isTextual()) {
                names.add(tool.asText());
            }
        }
        return names;
    }

    /** A list of tool names → the JSON array persisted in the {@code tools} column. */
    private JsonNode toolsJson(List<String> tools) {
        ArrayNode array = objectMapper.createArrayNode();
        if (tools != null) {
            tools.forEach(array::add);
        }
        return array;
    }
}
