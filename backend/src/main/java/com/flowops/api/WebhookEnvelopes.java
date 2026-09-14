package com.flowops.api;

/** Object envelope for the webhook management endpoint (never a bare value/null). */
public final class WebhookEnvelopes {

    private WebhookEnvelopes() {
    }

    /** {@code webhook} is null when the workflow has no webhook configured yet. */
    public record Webhook(WebhookResponse webhook) {
    }
}
