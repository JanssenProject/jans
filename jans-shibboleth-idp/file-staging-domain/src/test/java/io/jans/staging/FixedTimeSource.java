package io.jans.staging;

import io.jans.staging.port.TimeSource;

import java.time.Instant;

/** A settable clock so tests can advance time across the TTL boundary. */
final class FixedTimeSource implements TimeSource {

    private Instant now;

    FixedTimeSource(Instant now) {

        this.now = now;
    }

    void set(Instant now) {

        this.now = now;
    }

    @Override
    public Instant now() {

        return now;
    }
}
