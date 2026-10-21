/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

/**
 * Filter for a policy store release search. Every member is optional; a {@code null} member does
 * not restrict the result. The receipt-time bounds are inclusive epoch milliseconds.
 */
public final class PolicyRecordQuery {

	private final String policyStoreId;

	private final String policyStoreVersion;

	private final String bundleHash;

	private final Long receivedFromMs;

	private final Long receivedToMs;

	public PolicyRecordQuery(String policyStoreId, String policyStoreVersion, String bundleHash, Long receivedFromMs,
			Long receivedToMs) {
		this.policyStoreId = policyStoreId;
		this.policyStoreVersion = policyStoreVersion;
		this.bundleHash = bundleHash;
		this.receivedFromMs = receivedFromMs;
		this.receivedToMs = receivedToMs;
	}

	public String getPolicyStoreId() {
		return policyStoreId;
	}

	public String getPolicyStoreVersion() {
		return policyStoreVersion;
	}

	public String getBundleHash() {
		return bundleHash;
	}

	public Long getReceivedFromMs() {
		return receivedFromMs;
	}

	public Long getReceivedToMs() {
		return receivedToMs;
	}

	public boolean matches(StoredTraceRecord stored) {
		PolicyRef policy = stored.getPolicy();
		if (policy == null) {
			return false;
		}
		long receivedAtMs = stored.getReceipt().getReceivedAtMs();
		return (policyStoreId == null || policyStoreId.equals(policy.getPolicyStoreId()))
				&& (policyStoreVersion == null || policyStoreVersion.equals(policy.getPolicyStoreVersion()))
				&& (bundleHash == null || bundleHash.equals(policy.getBundleHash()))
				&& (receivedFromMs == null || receivedAtMs >= receivedFromMs)
				&& (receivedToMs == null || receivedAtMs <= receivedToMs);
	}

	@Override
	public String toString() {
		return "PolicyRecordQuery [policyStoreId=" + policyStoreId + ", policyStoreVersion=" + policyStoreVersion
				+ ", bundleHash=" + bundleHash + ", receivedFromMs=" + receivedFromMs + ", receivedToMs="
				+ receivedToMs + "]";
	}

}
