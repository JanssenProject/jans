// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * Lock's receipt time, always {@code YYYY-MM-DDTHH:MM:SS.sssZ} for a real UTC instant, so the
 * hashed text is the text an independent verifier reproduces. Immutable.
 */
public final class ReceivedAt {

    private final io.jans.trace.uniffi.ReceivedAt inner;

    ReceivedAt(io.jans.trace.uniffi.ReceivedAt inner) {
        this.inner = inner;
    }

    /** Formats Lock's clock reading, for example {@code System.currentTimeMillis()}. */
    public static ReceivedAt fromUnixMillis(long unixMillis) throws ReceivedAtRangeException {
        try {
            return new ReceivedAt(io.jans.trace.uniffi.ReceivedAt.Companion.fromUnixMillis(unixMillis));
        } catch (io.jans.trace.uniffi.ReceivedAtRangeException e) {
            throw new ReceivedAtRangeException(e.code(), e.toString());
        }
    }

    /** Only text that formats back unchanged, so impossible dates such as February 30 fail. */
    public static ReceivedAt parse(String text) throws ParseReceivedAtException {
        Objects.requireNonNull(text, "text");
        try {
            return new ReceivedAt(io.jans.trace.uniffi.ReceivedAt.Companion.parse(text));
        } catch (io.jans.trace.uniffi.ParseReceivedAtException e) {
            throw new ParseReceivedAtException(e.code(), e.toString());
        }
    }

    io.jans.trace.uniffi.ReceivedAt inner() {
        return inner;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReceivedAt && inner.equals(((ReceivedAt) other).inner);
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    /** The timestamp text. */
    @Override
    public String toString() {
        return inner.toString();
    }
}
