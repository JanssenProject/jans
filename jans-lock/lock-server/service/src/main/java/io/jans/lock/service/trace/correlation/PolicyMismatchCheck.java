/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import io.jans.core.cedarling.service.policy.PolicyDownloadService;
import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.core.cedarling.service.policy.PolicySnapshotEvent;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.correlation.PolicyReleaseHistory.ServedRelease;
import io.jans.lock.service.trace.model.PolicyRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Compares a record's {@code trace.policy} release with the policy store archives this node has
 * served under the same {@code policy_store_id}. A store Lock does not serve is never a
 * mismatch, because producers may load stores from elsewhere. A release Lock stopped serving
 * stays acceptable for {@code policyMismatchGraceSeconds} after it was retired, measured
 * against the record's {@code signed_at}, so records signed during a rollout are not flagged.
 * Releases are recorded when {@link PolicyDownloadService} publishes them; each check also
 * records what is served at that moment, in case a publication was missed. The check is
 * best-effort: a failure never rejects or flags a record.
 */
@ApplicationScoped
public class PolicyMismatchCheck {

	@Inject
	private Logger log;

	@Inject
	private PolicyDownloadService policyDownloadService;

	@Inject
	private AppConfiguration appConfiguration;

	private final PolicyReleaseHistory history = new PolicyReleaseHistory();

	private Clock clock = Clock.systemUTC();

	/**
	 * Records the releases served before and after a publication, retiring at the publication
	 * time those no longer served.
	 */
	public void onPolicySnapshot(@Observes PolicySnapshotEvent event) {
		try {
			history.observe(event.getPrevious(), event.getPublishedAtMs());
			history.observe(event.getCurrent(), event.getPublishedAtMs());
		} catch (RuntimeException ex) {
			log.warn("TRACE policy release history not updated", ex);
		}
	}

	/**
	 * @param signedAtSeconds the record's {@code trace.signed_at}
	 * @return {@code true} when the record names a served store with a release this node did not
	 *         serve at or shortly before {@code signed_at}
	 */
	public boolean check(PolicyRef policy, long signedAtSeconds) {
		if (policy == null) {
			return false;
		}
		try {
			Map<String, LoadedPolicySource> served = policyDownloadService.getLoadedPolicies();
			history.observe(served, clock.millis());
			return isMismatch(policy, signedAtSeconds, served);
		} catch (RuntimeException ex) {
			log.warn("TRACE policy release check skipped: policy_store_id={}", policy.getPolicyStoreId(), ex);
			return false;
		}
	}

	private boolean isMismatch(PolicyRef policy, long signedAtSeconds, Map<String, LoadedPolicySource> served) {
		LoadedPolicySource source = served.get(policy.getPolicyStoreId());
		if (source == null || !source.isArchive() || source.getBundleHash() == null) {
			return false;
		}
		Optional<ServedRelease> found = history.find(policy.getPolicyStoreId(), policy.getBundleHash());
		if (!found.isPresent()) {
			return true;
		}
		ServedRelease release = found.get();
		long graceMs = appConfiguration.getTraceConfiguration().getPolicyMismatchGraceSeconds() * 1000L;
		if (!release.isCurrent() && release.getRetiredAtMs() + graceMs < signedAtSeconds * 1000L) {
			return true;
		}
		return release.getVersion() != null && !release.getVersion().equals(policy.getPolicyStoreVersion());
	}

}
