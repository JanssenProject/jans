package io.jans.staging.adapter;

import io.jans.staging.Token;
import io.jans.staging.port.TokenGenerator;

import java.util.UUID;

/**
 * {@link TokenGenerator} minting random UUID (v4) tokens. A UUID string is never blank, so the
 * domain's {@code Token.of} validation always succeeds here.
 */
public final class UuidTokenGenerator implements TokenGenerator {

    @Override
    public Token generate() {

        return Token.of(UUID.randomUUID().toString()).getValue();
    }
}
