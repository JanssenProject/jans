package io.jans.staging.adapter;

import io.jans.staging.port.TimeSource;

import java.time.Instant;

/**
 * {@link TimeSource} backed by the system clock.
 */
public final class SystemTimeSource implements TimeSource {

    @Override
    public Instant now() {

        return Instant.now();
    }
}
