/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.Map;

import io.jans.core.cedarling.service.policy.PolicyDownloadService;
import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.PolicyRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Compares a record's {@code trace.policy} release with the policy store archive Lock serves under
 * the same {@code policy_store_id}. A store Lock does not serve is never a mismatch, because
 * producers may load stores from elsewhere.
 */
@ApplicationScoped
public class PolicyMismatchCheck {

	public static final String REASON_POLICY_MISMATCH = "policy_mismatch:trace.policy";

	@Inject
	private PolicyDownloadService policyDownloadService;

	@Inject
	private AppConfiguration appConfiguration;

	/**
	 * @return {@code true} when the record names a served store with a different release
	 * @throws TraceValidationException {@code invalid_assertion} on a mismatch when
	 *                                  {@code rejectPolicyMismatch} is enabled
	 */
	public boolean check(PolicyRef policy) {
		if (policy == null) {
			return false;
		}
		boolean mismatch = isMismatch(policy, policyDownloadService.getLoadedPolicies());
		if (mismatch && appConfiguration.getTraceConfiguration().isRejectPolicyMismatch()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					REASON_POLICY_MISMATCH);
		}
		return mismatch;
	}

	static boolean isMismatch(PolicyRef policy, Map<String, LoadedPolicySource> served) {
		LoadedPolicySource source = served.get(policy.getPolicyStoreId());
		if (source == null || !source.isArchive() || source.getBundleHash() == null) {
			return false;
		}
		if (!source.getBundleHash().equals(policy.getBundleHash())) {
			return true;
		}
		String servedVersion = source.getPolicyStoreVersion();
		return servedVersion != null && !servedVersion.equals(policy.getPolicyStoreVersion());
	}

}
