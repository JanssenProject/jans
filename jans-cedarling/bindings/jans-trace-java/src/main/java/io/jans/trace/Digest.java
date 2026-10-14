// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/** SHA-256 digest in the TRACE text form {@code sha256:<64 lowercase hex>}. Immutable. */
public final class Digest {

    private final io.jans.trace.uniffi.Digest inner;

    Digest(io.jans.trace.uniffi.Digest inner) {
        this.inner = inner;
    }

    /** Strict parse: anything but {@code sha256:} and 64 lowercase hex digits is rejected. */
    public static Digest parse(String text) throws ParseDigestException {
        Objects.requireNonNull(text, "text");
        try {
            return new Digest(io.jans.trace.uniffi.Digest.Companion.parse(text));
        } catch (io.jans.trace.uniffi.ParseDigestException e) {
            throw new ParseDigestException(Errors.code(e), e.getMessage());
        }
    }

    /** The all-zero sentinel that precedes the first link of a producer or receipt chain. */
    public static Digest genesis() {
        return new Digest(io.jans.trace.uniffi.Digest.Companion.genesis());
    }

    public byte[] toBytes() {
        return inner.toBytes();
    }

    io.jans.trace.uniffi.Digest inner() {
        return inner;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Digest && inner.equals(((Digest) other).inner);
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    /** The TRACE text form. */
    @Override
    public String toString() {
        return inner.toString();
    }
}
