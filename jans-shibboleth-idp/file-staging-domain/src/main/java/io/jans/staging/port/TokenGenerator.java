package io.jans.staging.port;

import io.jans.staging.Token;

/**
 * Mints fresh, opaque staging tokens. A port so the domain stays deterministic (no randomness in the
 * core) and testable; the adapter supplies the real generator.
 */
public interface TokenGenerator {

    Token generate();
}
