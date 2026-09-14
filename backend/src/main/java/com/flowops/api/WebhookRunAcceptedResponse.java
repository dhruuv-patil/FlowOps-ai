package com.flowops.api;

import java.util.UUID;

/**
 * The {@code 202 Accepted} body returned to an external caller when an inbound webhook
 * successfully starts a run. Carries only the new execution id and a coarse status —
 * nothing tenant-identifying, since the caller authenticated with a token, not a login.
 */
public record WebhookRunAcceptedResponse(UUID executionId, String status) {
}
