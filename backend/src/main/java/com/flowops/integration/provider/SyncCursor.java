package com.flowops.integration.provider;

/**
 * Opaque pagination cursor for incremental sync.
 * The meaning is provider-specific (n8n: last execution id sent as afterId).
 */
public record SyncCursor(String value) {

    public SyncCursor {
        if (value == null) {
            throw new IllegalArgumentException("cursor value cannot be null");
        }
    }

    public static final SyncCursor EMPTY = new SyncCursor("");

    public boolean isEmpty() {
        return value.isBlank();
    }
}