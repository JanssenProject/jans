/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * A producer chain's external identity: {@code (evidence_domain_id, producer_id,
 * producer_instance_id, producer_chain_id)} (design decision D-6, key type {@code chain}; design
 * §8). Must be pre-registered before any record may reference it (design decision D-5).
 *
 * @author Yuriy Movchan
 */
public final class ChainIdentity {

	private final String domainId;

	private final String producerId;

	private final String producerInstanceId;

	private final String producerChainId;

	public ChainIdentity(String domainId, String producerId, String producerInstanceId, String producerChainId) {
		this.domainId = Objects.requireNonNull(domainId, "domainId");
		this.producerId = Objects.requireNonNull(producerId, "producerId");
		this.producerInstanceId = Objects.requireNonNull(producerInstanceId, "producerInstanceId");
		this.producerChainId = Objects.requireNonNull(producerChainId, "producerChainId");
	}

	public String getDomainId() {
		return domainId;
	}

	public String getProducerId() {
		return producerId;
	}

	public String getProducerInstanceId() {
		return producerInstanceId;
	}

	public String getProducerChainId() {
		return producerChainId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChainIdentity)) {
			return false;
		}
		ChainIdentity other = (ChainIdentity) o;
		return domainId.equals(other.domainId) && producerId.equals(other.producerId)
				&& producerInstanceId.equals(other.producerInstanceId)
				&& producerChainId.equals(other.producerChainId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(domainId, producerId, producerInstanceId, producerChainId);
	}

	@Override
	public String toString() {
		return "ChainIdentity [domainId=" + domainId + ", producerId=" + producerId + ", producerInstanceId="
				+ producerInstanceId + ", producerChainId=" + producerChainId + "]";
	}

}
