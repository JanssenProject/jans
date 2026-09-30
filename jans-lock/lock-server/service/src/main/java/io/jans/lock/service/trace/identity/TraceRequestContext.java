/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The resolved, immutable context of one TRACE request: which client submitted it, which evidence
 * domain it is bound to, which producer ids it may forward records for, and when/where Lock
 * received it (design decisions D-1, D-2, D-3).
 *
 * @author Yuriy Movchan
 */
public class TraceRequestContext {

	private final String clientId;

	private final String evidenceDomainId;

	private final List<String> allowedProducerIds;

	private final long receivedAtMs;

	private final String nodeId;

	public TraceRequestContext(String clientId, String evidenceDomainId, List<String> allowedProducerIds,
			long receivedAtMs, String nodeId) {
		this.clientId = clientId;
		this.evidenceDomainId = evidenceDomainId;
		this.allowedProducerIds = allowedProducerIds == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(allowedProducerIds));
		this.receivedAtMs = receivedAtMs;
		this.nodeId = nodeId;
	}

	/**
	 * @return the authenticated OAuth client id (design decision D-2)
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @return the evidence domain the client is bound to (design decision D-1)
	 */
	public String getEvidenceDomainId() {
		return evidenceDomainId;
	}

	/**
	 * @return the producer ids the client may submit records for; {@code ["*"]} means any producer
	 *         (design decision D-3)
	 */
	public List<String> getAllowedProducerIds() {
		return allowedProducerIds;
	}

	/**
	 * @return the epoch-millisecond instant this Lock node resolved the request's identity/domain
	 */
	public long getReceivedAtMs() {
		return receivedAtMs;
	}

	/**
	 * @return the identifier of the Lock node that received the request
	 */
	public String getNodeId() {
		return nodeId;
	}

	@Override
	public String toString() {
		return "TraceRequestContext [clientId=" + clientId + ", evidenceDomainId=" + evidenceDomainId
				+ ", allowedProducerIds=" + allowedProducerIds + ", receivedAtMs=" + receivedAtMs + ", nodeId="
				+ nodeId + "]";
	}

}
