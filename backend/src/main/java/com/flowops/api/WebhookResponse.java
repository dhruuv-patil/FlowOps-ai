package com.flowops.api;

import java.time.Instant;

/**
 * A workflow's inbound-webhook state as returned to the client. Masked by design: it
 * carries the non-secret {@code tokenHint} (last-4) and a {@code urlMasked} for display,
 * but <em>never</em> the token or a working URL — that is shown exactly once, at
 * generate time, via {@link WebhookSecretResponse}.
 */
public record WebhookResponse(boolean enabled, String tokenHint, String urlMasked, Instant createdAt) {
}
