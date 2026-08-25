package com.flowops.common.ratelimit;

import com.flowops.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory fixed-window throttling (API contract §8).
 *
 * <p>Per-instance and deliberately so for M1 — a Redis-backed limiter is opt-in
 * later. Keys are built from the <em>normalized</em> email so casing tricks cannot
 * reset a window, and from {@code getRemoteAddr()} only: honouring
 * {@code X-Forwarded-For} without a trusted proxy in front would make the limit a
 * one-header bypass.
 */
@Component
public class RateLimiter {

    /** Above this many live windows, expired entries are swept before inserting. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Counts one hit against {@code key} and throws {@code 429 RATE_LIMITED} once
     * the window's allowance is exhausted.
     */
    public void check(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        if (windows.size() > SWEEP_THRESHOLD) {
            sweep(now, windowMillis);
        }

        Window counter = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (counter) {
            if (now - counter.startedAt >= windowMillis) {
                counter.startedAt = now;
                counter.count = 0;
            }
            counter.count++;
            if (counter.count > limit) {
                long remaining = windowMillis - (now - counter.startedAt);
                throw ApiException.rateLimited(Math.max(1, (remaining + 999) / 1000));
            }
        }
    }

    /** The client address, never a client-supplied forwarding header. */
    public static String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private void sweep(long now, long windowMillis) {
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Window candidate = it.next().getValue();
            synchronized (candidate) {
                if (now - candidate.startedAt >= windowMillis) {
                    it.remove();
                }
            }
        }
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
