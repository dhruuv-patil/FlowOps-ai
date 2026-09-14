package com.flowops.api;

/**
 * The one-time response to generating (or rotating) a webhook: the full working
 * {@code url} with the token embedded. It is returned exactly once and can never be
 * recovered afterwards — only the hash is stored. The client must surface a "copy it
 * now" warning; subsequent reads see only {@link WebhookResponse} (masked).
 */
public record WebhookSecretResponse(String url, String tokenHint, boolean enabled) {
}
