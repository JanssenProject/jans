package io.jans.staging;

import io.jans.staging.port.TokenGenerator;

/** Deterministic token generator (tok-1, tok-2, …) for repeatable service tests. */
final class SequentialTokenGenerator implements TokenGenerator {

    private int counter;

    @Override
    public Token generate() {

        return Token.of("tok-" + (++counter)).getValue();
    }
}
