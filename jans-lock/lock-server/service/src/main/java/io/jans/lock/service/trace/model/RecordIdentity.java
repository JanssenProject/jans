/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * A TRACE record's external identity: {@code (evidence_domain_id, producer_id, record_id)}
 * (design decision D-6, key type {@code rec}). Two records with the same identity but a different
 * {@code content_digest} are a conflict (design decision D-8 step 5).
 *
 * @author Yuriy Movchan
 */
public final class RecordIdentity {

	private final String domainId;

	private final String producerId;

	private final String recordId;

	public RecordIdentity(String domainId, String producerId, String recordId) {
		this.domainId = Objects.requireNonNull(domainId, "domainId");
		this.producerId = Objects.requireNonNull(producerId, "producerId");
		this.recordId = Objects.requireNonNull(recordId, "recordId");
	}

	public String getDomainId() {
		return domainId;
	}

	public String getProducerId() {
		return producerId;
	}

	public String getRecordId() {
		return recordId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RecordIdentity)) {
			return false;
		}
		RecordIdentity other = (RecordIdentity) o;
		return domainId.equals(other.domainId) && producerId.equals(other.producerId)
				&& recordId.equals(other.recordId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(domainId, producerId, recordId);
	}

	@Override
	public String toString() {
		return "RecordIdentity [domainId=" + domainId + ", producerId=" + producerId + ", recordId=" + recordId
				+ "]";
	}

}
