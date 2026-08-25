package com.flowops.domain;

/**
 * How a run was started. {@code MANUAL} is a user clicking Run; {@code WEBHOOK} is
 * an inbound HTTP call to the workflow's webhook URL (wired in M5). The trigger
 * node in the graph must match: a manual run needs a {@code manual_trigger}.
 */
public enum TriggerType {
    MANUAL,
    WEBHOOK
}
