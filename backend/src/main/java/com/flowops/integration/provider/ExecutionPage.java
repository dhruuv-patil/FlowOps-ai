package com.flowops.integration.provider;

import java.util.List;

/**
 * Paginated result of fetching executions from a provider.
 */
public record ExecutionPage<T>(
        List<T> items,
        SyncCursor nextCursor,
        boolean hasMore
) {
    public ExecutionPage {
        if (items == null) {
            throw new IllegalArgumentException("items cannot be null");
        }
        if (nextCursor == null) {
            throw new IllegalArgumentException("nextCursor cannot be null");
        }
    }

    public static <T> ExecutionPage<T> empty() {
        return new ExecutionPage<>(List.of(), SyncCursor.EMPTY, false);
    }
}