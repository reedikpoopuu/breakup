package com.example.demo.contract;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed-window per-user limiter on {@code POST /api/contracts/parse} - the one
 * endpoint in this app that's both resource-heavy (synchronous PDF parsing) and
 * directly cost-bearing (triggers a billed AI provider call), and reachable by any
 * authenticated {@code USER}, not just admins. Without this, one account could drive
 * unlimited AI spend and worker-thread load just by repeatedly re-uploading a large
 * PDF - there was previously no rate limiting anywhere in the app.
 * <p>
 * In-memory and per-instance, which matches how the rest of this app runs today (a
 * single H2-file-backed instance, no shared cache) - if this ever runs as more than one
 * instance, the limit becomes "N requests per user per instance" rather than a true
 * global limit, which is worth revisiting at that point (a shared store, e.g. Redis,
 * would be the natural next step), not before.
 */
@Component
public class ContractParseRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private record Window(Instant windowStart, AtomicInteger count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** True if this caller is still within their quota for the current window. */
    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || Duration.between(existing.windowStart(), now).compareTo(WINDOW) >= 0) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= MAX_REQUESTS_PER_WINDOW;
    }
}
