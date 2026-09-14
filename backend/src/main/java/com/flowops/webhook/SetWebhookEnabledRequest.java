package com.flowops.webhook;

import jakarta.validation.constraints.NotNull;

/** Body for enabling or disabling a workflow's inbound webhook. */
public record SetWebhookEnabledRequest(@NotNull Boolean enabled) {
}
