package com.flowops.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.api.WebhookRunAcceptedResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.common.ratelimit.RateLimiter;
import com.flowops.execution.ExecutionService;
import com.flowops.repository.IdempotencyKeyRepository;
import com.flowops.domain.IdempotencyKey;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public, JWT-less inbound-webhook endpoint: {@code POST /api/webhooks/{workflowId}/{token}}.
 *
 * <p>The token in the URL <em>is</em> the authentication; there is no principal on this
 * path (it is the one place a run starts without a {@link com.flowops.security.FlowOpsPrincipal}).
 * {@code SecurityConfig} opens the POST route with two single-segment wildcards, so only
 * {@code {workflowId}/{token}} under {@code /api/webhooks/} is public.
 *
 * <p>Defenses, in order: parse the workflow id (a malformed id is treated as a miss, not
 * a distinct error); a per-workflow rate limit; a hard request-size cap read with a
 * bounded stream so a lying {@code Content-Length} cannot exhaust memory; then token
 * verification. Every authentication-shaped failure — unknown workflow, no webhook,
 * disabled, wrong token, unpublished, or a graph with no webhook trigger — returns the
 * <em>same</em> opaque {@code 404}; only a size violation ({@code 413}) and an unparseable
 * body ({@code 400}) are distinct, because neither leaks whether the token was valid. The
 * token is never logged.
 *
 * <p>Idempotency: callers may provide an {@code Idempotency-Key} header to safely retry
 * webhook calls. If a key is provided and matches a previously accepted request within the
 * TTL (24h), the existing execution id is returned without creating a duplicate run.
 * This prevents accidental duplicate executions from network retries or duplicate events.
 */
@RestController
public class WebhookIngressController {

    /** Per-workflow allowance per minute. Generous for legitimate event sources, low enough
     * to blunt brute-forcing a token or hammering the run pipeline. */
    private static final int RATE_LIMIT = 60;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    /** Trigger payloads are small; 64 KiB is ample and bounds the memory a caller can force. */
    private static final int MAX_BODY_BYTES = 64 * 1024;

    /** Idempotency key TTL — 24 hours. */
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final RateLimiter rateLimiter;
    private final WebhookService webhookService;
    private final ExecutionService executionService;
    private final ObjectMapper mapper;
    private final IdempotencyKeyRepository idempotencyKeys;

    public WebhookIngressController(
            RateLimiter rateLimiter,
            WebhookService webhookService,
            ExecutionService executionService,
            ObjectMapper mapper,
            IdempotencyKeyRepository idempotencyKeys) {
        this.rateLimiter = rateLimiter;
        this.webhookService = webhookService;
        this.executionService = executionService;
        this.mapper = mapper;
        this.idempotencyKeys = idempotencyKeys;
    }

    @PostMapping("/api/webhooks/{workflowId}/{token}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WebhookRunAcceptedResponse receive(
            @PathVariable("workflowId") String workflowIdRaw,
            @PathVariable("token") String token,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request) {
        UUID workflowId = parseWorkflowId(workflowIdRaw);

        // Rate-limit before touching the DB or reading the body.
        rateLimiter.check("webhook:" + workflowId, RATE_LIMIT, RATE_WINDOW);

        byte[] body = readCappedBody(request);

        // Trusted org comes from the stored row, never from the request (contract §2).
        UUID organizationId = webhookService.verifyAndResolve(workflowId, token);

        // Idempotency: if a key is provided, check for existing execution
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = "webhook:" + workflowId + ":" + idempotencyKey;
            Optional<IdempotencyKey> existing = idempotencyKeys.findByKey(key);
            if (existing.isPresent()) {
                // Return existing execution id — safe replay, no duplicate run created
                return new WebhookRunAcceptedResponse(existing.get().getExecutionId(), "QUEUED");
            }
        }

        JsonNode payload = parseBody(body);
        UUID executionId = executionService.startWebhookRun(workflowId, organizationId, payload);

        // Store idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = "webhook:" + workflowId + ":" + idempotencyKey;
            idempotencyKeys.save(IdempotencyKey.create(key, executionId, IDEMPOTENCY_TTL));
        }

        return new WebhookRunAcceptedResponse(executionId, "QUEUED");
    }

    /** A malformed workflow id is a miss, not a distinct error — same opaque 404. */
    private UUID parseWorkflowId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException notAUuid) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
    }

    /**
     * Reads the body with a hard cap. A declared {@code Content-Length} over the cap is
     * rejected immediately; otherwise we read one byte past the cap and reject if the
     * stream actually exceeded it — so a lying/absent length cannot force unbounded
     * buffering. A size rejection reveals nothing about the token, so it is a clear
     * {@code 413}, not the opaque 404.
     */
    private byte[] readCappedBody(HttpServletRequest request) {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        try (InputStream in = request.getInputStream()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE);
            }
            return bytes;
        } catch (IOException unreadable) {
            throw new ApiException(ErrorCode.MALFORMED_REQUEST);
        }
    }

    /** Empty/blank body → an empty object (a valid trigger payload); anything present must be JSON. */
    private JsonNode parseBody(byte[] body) {
        if (body.length == 0) {
            return mapper.createObjectNode();
        }
        try {
            JsonNode node = mapper.readTree(body);
            return node == null || node.isMissingNode() ? mapper.createObjectNode() : node;
        } catch (IOException notJson) {
            throw new ApiException(ErrorCode.MALFORMED_REQUEST);
        }
    }
}
