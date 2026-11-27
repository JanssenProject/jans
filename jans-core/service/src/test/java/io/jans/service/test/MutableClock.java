/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UTC clock for tests that stays at a fixed instant until it is moved. Thread safe.
 */
public class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;

    public MutableClock(Instant instant) {
        this.instant = new AtomicReference<>(instant);
    }

    public void setInstant(Instant value) {
        instant.set(value);
    }

    public Instant advance(Duration duration) {
        return instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("MutableClock is always UTC");
    }

    @Override
    public Instant instant() {
        return instant.get();
    }

}
