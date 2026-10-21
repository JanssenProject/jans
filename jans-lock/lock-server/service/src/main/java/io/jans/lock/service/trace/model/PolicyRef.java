/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * The policy store release a record names in {@code trace.policy}: {@code policy_store_id},
 * {@code policy_store_version} and {@code bundle_hash}.
 */
public final class PolicyRef {

	private final String policyStoreId;

	private final String policyStoreVersion;

	private final String bundleHash;

	public PolicyRef(String policyStoreId, String policyStoreVersion, String bundleHash) {
		this.policyStoreId = Objects.requireNonNull(policyStoreId, "policyStoreId");
		this.policyStoreVersion = Objects.requireNonNull(policyStoreVersion, "policyStoreVersion");
		this.bundleHash = Objects.requireNonNull(bundleHash, "bundleHash");
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PolicyRef)) {
			return false;
		}
		PolicyRef other = (PolicyRef) o;
		return policyStoreId.equals(other.policyStoreId) && policyStoreVersion.equals(other.policyStoreVersion)
				&& bundleHash.equals(other.bundleHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(policyStoreId, policyStoreVersion, bundleHash);
	}

	@Override
	public String toString() {
		return "PolicyRef [policyStoreId=" + policyStoreId + ", policyStoreVersion=" + policyStoreVersion
				+ ", bundleHash=" + bundleHash + "]";
	}

}
