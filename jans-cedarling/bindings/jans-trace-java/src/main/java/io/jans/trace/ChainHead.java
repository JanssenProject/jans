// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import java.util.Objects;

/**
 * The end of an evidence-domain chain verified so far: only the last sequence and hash, because
 * each receipt hash covers every receipt before it. Walk a stored chain from {@link #empty} with
 * {@code head = head.append(receipt)}, then compare the final head with the one Lock stored, so
 * a cut-off tail is found too. Immutable.
 */
public final class ChainHead {

    private final io.jans.trace.uniffi.ChainHead inner;

    private ChainHead(io.jans.trace.uniffi.ChainHead inner) {
        this.inner = inner;
    }

    /** A chain with no receipts: sequence 0 and the sentinel, so the next must be the genesis receipt. */
    public static ChainHead empty(String evidenceDomainId) {
        Objects.requireNonNull(evidenceDomainId, "evidenceDomainId");
        return new ChainHead(io.jans.trace.uniffi.ChainHead.Companion.empty(evidenceDomainId));
    }

    /**
     * Continues a walk from a head saved earlier. Trusts that state: keep it where the operator of
     * the audited chain cannot write, or it can skip every receipt before it.
     */
    public static ChainHead resume(String evidenceDomainId, long sequence, Digest hash) throws ChainHeadException {
        Objects.requireNonNull(evidenceDomainId, "evidenceDomainId");
        Objects.requireNonNull(hash, "hash");
        try {
            return new ChainHead(io.jans.trace.uniffi.ChainHead.Companion.resume(evidenceDomainId, sequence, hash.inner()));
        } catch (io.jans.trace.uniffi.ChainHeadException e) {
            throw new ChainHeadException(e.code(), e.toString());
        }
    }

    /** Checks the evidence domain, the next sequence and this head as predecessor. */
    public ChainHead append(ReceiptCommitment receipt) throws ReceiptLinkException {
        Objects.requireNonNull(receipt, "receipt");
        try {
            return new ChainHead(inner.append(receipt.inner()));
        } catch (io.jans.trace.uniffi.ReceiptLinkException e) {
            throw new ReceiptLinkException(e.code(), e.toString());
        }
    }

    public String evidenceDomainId() {
        return inner.evidenceDomainId();
    }

    /** 0 while the chain is empty. */
    public long sequence() {
        return inner.sequence();
    }

    /** {@link Digest#genesis()} while the chain is empty. */
    public Digest hash() {
        return new Digest(inner.hash());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ChainHead && inner.equals(((ChainHead) other).inner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence(), hash());
    }

    /** Sequence and hash; the evidence-domain id is left out. */
    @Override
    public String toString() {
        return "ChainHead[" + sequence() + ", " + hash() + "]";
    }
}
