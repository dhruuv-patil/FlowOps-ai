package com.flowops.integration.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for {@link SyncCursor}. */
class SyncCursorTest {

    @Test
    void emptyIsEmpty() {
        assertThat(SyncCursor.EMPTY.isEmpty()).isTrue();
        assertThat(SyncCursor.EMPTY.value()).isEmpty();
    }

    @Test
    void nonEmptyIsNotEmpty() {
        SyncCursor cursor = new SyncCursor("execution-123");
        assertThat(cursor.isEmpty()).isFalse();
        assertThat(cursor.value()).isEqualTo("execution-123");
    }

    @Test
    void constructorNullThrows() {
        assertThatThrownBy(() -> new SyncCursor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cursor value cannot be null");
    }

    @Test
    void toStringIsRecordFormat() {
        SyncCursor cursor = new SyncCursor("abc");
        assertThat(cursor.toString()).isEqualTo("SyncCursor[value=abc]");
    }
}