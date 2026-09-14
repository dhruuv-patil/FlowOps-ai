package com.flowops.integration.delivery;

import java.util.Map;

/**
 * A message ready for delivery through a notification provider. {@code channel} is a
 * display label for the node output; {@code text} is the rendered message body;
 * {@code payload} carries optional structured context (attachments, event payload)
 * that a provider may include.
 */
public record NotificationMessage(
        String channel,
        String text,
        Map<String, Object> payload) {
}