// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;
import io.jans.trace.TraceException.ReceiptPositionException;

/**
 * Where a receipt sits in its evidence-domain chain. Sequence 1 names the genesis sentinel and
 * no other sequence does, so a chain can neither start without it nor restart midway. Immutable.
 */
public final class ReceiptPosition {

    private final io.jans.trace.uniffi.ReceiptPosition inner;

    ReceiptPosition(io.jans.trace.uniffi.ReceiptPosition inner) {
        this.inner = inner;
    }

    /** The first receipt of an evidence domain. */
    public static ReceiptPosition genesis() {
        return new ReceiptPosition(io.jans.trace.uniffi.ReceiptPosition.Companion.genesis());
    }

    /**
     * Sequence and predecessor exactly as Lock assigned or stored them: {@code (head + 1, headHash)}
     * on ingest. Refuses pairs outside the v1 chain and sequences outside 1..2^53-1.
     */
    public static ReceiptPosition of(long sequence, Digest prevReceiptHash) throws ReceiptPositionException {
        Objects.requireNonNull(prevReceiptHash, "prevReceiptHash");
        try {
            return new ReceiptPosition(io.jans.trace.uniffi.ReceiptPosition.Companion.of(sequence, prevReceiptHash.inner()));
        } catch (io.jans.trace.uniffi.ReceiptPositionException e) {
            throw new ReceiptPositionException(e.code(), e.toString());
        }
    }

    public long sequence() {
        return inner.sequence();
    }

    public Digest prevReceiptHash() {
        return new Digest(inner.prevReceiptHash());
    }

    io.jans.trace.uniffi.ReceiptPosition inner() {
        return inner;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReceiptPosition && inner.equals(((ReceiptPosition) other).inner);
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    @Override
    public String toString() {
        return "ReceiptPosition[" + sequence() + ", " + prevReceiptHash() + "]";
    }
}
