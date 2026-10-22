/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bounds how much outbound work the readiness endpoint can cause, per node. Three limits, all constants:
 * <ul>
 * <li>a relying party is fetched at most once per {@link #MIN_RECHECK_INTERVAL_MS};</li>
 * <li>at most {@link #MAX_FETCHES_PER_WINDOW} files are fetched per {@link #WINDOW_MS}, whichever relying
 * parties they belong to;</li>
 * <li>at most {@link #MAX_CONCURRENT} checks run at the same time.</li>
 * </ul>
 * In a cluster each node applies its own limits, so the installation-wide ceiling is the number of nodes
 * times these values. A refused request is never turned into a fetch: the caller answers 429 with
 * {@code Retry-After}.
 */
@ApplicationScoped
public class ReadinessRateLimiter {

    public static final long MIN_RECHECK_INTERVAL_MS = 60_000;
    public static final long WINDOW_MS = 60_000;
    public static final int MAX_FETCHES_PER_WINDOW = 20;
    public static final int MAX_CONCURRENT = 3;

    private static final long CONCURRENCY_RETRY_MS = 1_000;

    private final LongSupplier clock;
    private final Map<String, Long> lastFetch = new HashMap<>();
    private long windowStart;
    private int windowFetches;
    private int running;

    public ReadinessRateLimiter() {
        this(System::currentTimeMillis);
    }

    ReadinessRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Asks to run a check that fetches {@code fetches} files for {@code rpId}.
     *
     * @return 0 when granted (the caller must then call {@link #release()}), otherwise the number of
     *         milliseconds to wait before asking again
     */
    public synchronized long tryAcquire(String rpId, int fetches) {
        long now = clock.getAsLong();
        Long last = lastFetch.get(rpId);
        if (last != null && now - last < MIN_RECHECK_INTERVAL_MS) {
            return MIN_RECHECK_INTERVAL_MS - (now - last);
        }
        if (now - windowStart >= WINDOW_MS) {
            windowStart = now;
            windowFetches = 0;
        }
        if (windowFetches + fetches > MAX_FETCHES_PER_WINDOW) {
            return windowStart + WINDOW_MS - now;
        }
        if (running >= MAX_CONCURRENT) {
            return CONCURRENCY_RETRY_MS;
        }
        lastFetch.put(rpId, now);
        windowFetches += fetches;
        running++;
        return 0;
    }

    public synchronized void release() {
        running--;
    }

    /** Earliest time, in epoch milliseconds, at which {@code rpId} may be fetched again. */
    public synchronized long nextAllowedAt(String rpId) {
        Long last = lastFetch.get(rpId);
        return (last == null ? clock.getAsLong() : last) + MIN_RECHECK_INTERVAL_MS;
    }
}
