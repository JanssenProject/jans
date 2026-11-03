/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static io.jans.fido2.service.readiness.ReadinessRateLimiter.MAX_CONCURRENT;
import static io.jans.fido2.service.readiness.ReadinessRateLimiter.MAX_FETCHES_PER_WINDOW;
import static io.jans.fido2.service.readiness.ReadinessRateLimiter.MIN_RECHECK_INTERVAL_MS;
import static io.jans.fido2.service.readiness.ReadinessRateLimiter.WINDOW_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReadinessRateLimiterTest {

    private long now = 5_000_000L;
    private final ReadinessRateLimiter limiter = new ReadinessRateLimiter(() -> now);

    @Test
    void sameRpInsideTheMinimumInterval_isRefusedWithTheRemainingWait() {
        assertEquals(0, limiter.tryAcquire("a.example", 1));
        limiter.release();
        now += 10_000;

        assertEquals(MIN_RECHECK_INTERVAL_MS - 10_000, limiter.tryAcquire("a.example", 1));
    }

    @Test
    void sameRpAfterTheMinimumInterval_isGranted() {
        assertEquals(0, limiter.tryAcquire("a.example", 1));
        limiter.release();
        now += MIN_RECHECK_INTERVAL_MS;

        assertEquals(0, limiter.tryAcquire("a.example", 1));
    }

    @Test
    void aBurstOfDistinctRps_cannotExceedTheWindowCap() {
        int granted = 0;
        for (int i = 0; i < 50; i++) {
            if (limiter.tryAcquire("rp" + i + ".example", 2) == 0) {
                granted += 2;
                limiter.release();
            }
        }

        assertEquals(MAX_FETCHES_PER_WINDOW, granted);
    }

    @Test
    void windowCap_resetsWhenTheWindowEnds() {
        for (int i = 0; i < MAX_FETCHES_PER_WINDOW; i++) {
            assertEquals(0, limiter.tryAcquire("rp" + i + ".example", 1));
            limiter.release();
        }
        long wait = limiter.tryAcquire("late.example", 1);
        assertTrue(wait > 0 && wait <= WINDOW_MS);

        now += WINDOW_MS;

        assertEquals(0, limiter.tryAcquire("late.example", 1));
    }

    @Test
    void concurrencyCap_refusesTheNextCheck_untilOneFinishes() {
        for (int i = 0; i < MAX_CONCURRENT; i++) {
            assertEquals(0, limiter.tryAcquire("rp" + i + ".example", 1));
        }

        assertTrue(limiter.tryAcquire("one-more.example", 1) > 0);

        limiter.release();

        assertEquals(0, limiter.tryAcquire("one-more.example", 1));
    }

    @Test
    void nextAllowedAt_isOneIntervalAfterTheLastFetch() {
        limiter.tryAcquire("a.example", 1);

        assertEquals(now + MIN_RECHECK_INTERVAL_MS, limiter.nextAllowedAt("a.example"));
    }
}
