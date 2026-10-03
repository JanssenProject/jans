package io.jans.staging.port;

import java.time.Instant;

/**
 * The clock the service reads. A port so expiry and reaping are deterministic under test.
 */
public interface TimeSource {

    Instant now();
}
