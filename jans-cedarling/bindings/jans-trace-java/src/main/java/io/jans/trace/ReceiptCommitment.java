// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;
import io.jans.trace.TraceException.ReceiptLinkException;
import io.jans.trace.TraceException.StoredReceiptException;

/**
 * The v1 field set hashed into a receipt. Every field is valid by its type, so any commitment
 * hashes; once built it cannot be changed to name another record. Immutable.
 */
public final class ReceiptCommitment {

    private final io.jans.trace.uniffi.ReceiptCommitment inner;

    private ReceiptCommitment(io.jans.trace.uniffi.ReceiptCommitment inner) {
        this.inner = inner;
    }

    /**
     * The commitment Lock appends for a record it just verified: producer, record id and content
     * digest come from the verification, so a receipt cannot name a record that was not checked.
     * Build it inside the transaction that locks the chain head.
     */
    public static ReceiptCommitment forAssertion(String evidenceDomainId, ReceiptPosition position,
            ReceivedAt receivedAt, VerifiedAssertion assertion) {
        Objects.requireNonNull(evidenceDomainId, "evidenceDomainId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(assertion, "assertion");
        return new ReceiptCommitment(io.jans.trace.uniffi.ReceiptCommitment.Companion.forAssertion(
                evidenceDomainId, position.inner(), receivedAt.inner(), assertion.inner()));
    }

    /** Rebuilds a commitment from stored values, to re-hash and re-link a chain in an audit. */
    public static ReceiptCommitment fromStored(StoredReceipt stored) throws StoredReceiptException {
        Objects.requireNonNull(stored, "stored");
        try {
            return new ReceiptCommitment(io.jans.trace.uniffi.ReceiptCommitment.Companion.fromStored(stored.inner()));
        } catch (io.jans.trace.uniffi.StoredReceiptException e) {
            throw new StoredReceiptException(e.code(), e.toString());
        }
    }

    public String evidenceDomainId() {
        return inner.evidenceDomainId();
    }

    public ReceiptPosition position() {
        return new ReceiptPosition(inner.position());
    }

    public ReceivedAt receivedAt() {
        return new ReceivedAt(inner.receivedAt());
    }

    public String producerId() {
        return inner.producerId();
    }

    public String recordId() {
        return inner.recordId();
    }

    public Digest contentDigest() {
        return new Digest(inner.contentDigest());
    }

    /** {@code sha256(JCS(commitment))}. */
    public Digest receiptHash() {
        return new Digest(inner.receiptHash());
    }

    /**
     * Checks that this receipt names {@code previousReceiptHash}, the current chain head (or
     * {@link Digest#genesis()} for an empty domain), and returns its own hash: the new head. To
     * audit a stored chain, walk it with {@link ChainHead}, which also checks domain and sequence.
     */
    public Digest verifyLink(Digest previousReceiptHash) throws ReceiptLinkException {
        Objects.requireNonNull(previousReceiptHash, "previousReceiptHash");
        try {
            return new Digest(inner.verifyLink(previousReceiptHash.inner()));
        } catch (io.jans.trace.uniffi.ReceiptLinkException e) {
            throw new ReceiptLinkException(e.code(), e.toString());
        }
    }

    io.jans.trace.uniffi.ReceiptCommitment inner() {
        return inner;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReceiptCommitment && inner.equals(((ReceiptCommitment) other).inner);
    }

    @Override
    public int hashCode() {
        return receiptHash().hashCode();
    }

    @Override
    public String toString() {
        return "ReceiptCommitment[" + position() + ", " + receiptHash() + "]";
    }
}
