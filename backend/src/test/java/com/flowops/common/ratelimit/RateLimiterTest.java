package com.flowops.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Focused unit test for the fixed-window limiter.
 *
 * <p>Lives outside the {@code @SpringBootTest} context (and outside the Mockito
 * agent path) so it runs in any environment. Integration tests disable the limiter
 * via {@code flowops.ratelimit.enabled: false}; this is where the window counting
 * itself is verified.
 */
class RateLimiterTest {

    @Test
    void allowsUpToTheLimitThenThrows() {
        RateLimiter limiter = new RateLimiter(true);

        for (int i = 0; i < 3; i++) {
            limiter.check("test:key", 3, Duration.ofMinutes(1));
        }

        assertThatThrownBy(() ->
                limiter.check("test:key", 3, Duration.ofMinutes(1)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.RATE_LIMITED);
    }

    @Test
    void windowsArePerKey() {
        RateLimiter limiter = new RateLimiter(true);

        limiter.check("key-a", 1, Duration.ofMinutes(1));
        // key-b has its own allowance.
        limiter.check("key-b", 1, Duration.ofMinutes(1));
        // key-a is now exhausted; key-b is not.
        assertThatThrownBy(() -> limiter.check("key-a", 1, Duration.ofMinutes(1)))
                .isInstanceOf(ApiException.class);
        limiter.check("key-b", 2, Duration.ofMinutes(1));
    }

    @Test
    void expiredWindowResetsTheCount() {
        RateLimiter limiter = new RateLimiter(true);

        limiter.check("key", 1, Duration.ofMillis(50));
        assertThatThrownBy(() -> limiter.check("key", 1, Duration.ofMillis(50)))
                .isInstanceOf(ApiException.class);

        // Sleep past the window; the count resets.
        try {
            Thread.sleep(80);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        limiter.check("key", 1, Duration.ofMillis(50));
    }

    @Test
    void disabledLimiterIsANoOp() {
        RateLimiter limiter = new RateLimiter(false);

        // A single-window limit far below the call count: were the limiter active,
        // the very first over-limit call would throw 429.
        for (int i = 0; i < 100; i++) {
            limiter.check("test:key", 1, Duration.ofMinutes(1));
        }
    }
}