/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * A position within a producer chain: {@link ChainIdentity} plus {@code sequence_number} (design
 * decision D-6, key type {@code pos}; design §8). More than one record may share a
 * {@code ChainPosition} — that is an equivocation, not a duplicate (design decision D-9).
 *
 * @author Yuriy Movchan
 */
public final class ChainPosition {

	private final ChainIdentity chainIdentity;

	private final long sequenceNumber;

	public ChainPosition(ChainIdentity chainIdentity, long sequenceNumber) {
		this.chainIdentity = Objects.requireNonNull(chainIdentity, "chainIdentity");
		this.sequenceNumber = sequenceNumber;
	}

	public ChainIdentity getChainIdentity() {
		return chainIdentity;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChainPosition)) {
			return false;
		}
		ChainPosition other = (ChainPosition) o;
		return sequenceNumber == other.sequenceNumber && chainIdentity.equals(other.chainIdentity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(chainIdentity, sequenceNumber);
	}

	@Override
	public String toString() {
		return "ChainPosition [chainIdentity=" + chainIdentity + ", sequenceNumber=" + sequenceNumber + "]";
	}

}
