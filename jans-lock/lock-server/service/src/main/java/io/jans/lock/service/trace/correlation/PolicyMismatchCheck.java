/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import io.jans.core.cedarling.service.policy.PolicyDownloadService;
import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.core.cedarling.service.policy.PolicySnapshotEvent;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.store.PolicyReleaseServed;
import io.jans.lock.service.trace.store.TraceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Compares a record's {@code trace.policy} release with the policy store archive releases Lock
 * has served under the same {@code policy_store_id}. Every node records the releases it serves in
 * the shared TRACE store when it publishes a reload, so the history covers all replicas and
 * survives restarts. A release is acceptable when this node serves it now, or when some node
 * served it within the effective grace period before the time the record is judged at. A store
 * this node does not serve is never a mismatch, because producers may load stores from
 * elsewhere. The check is best-effort: a failure never rejects or flags a record.
 */
@ApplicationScoped
public class PolicyMismatchCheck {

	static final long CACHE_TTL_MS = 10_000L;

	static final int CACHE_MAX_ENTRIES = 1_000;

	private static final long MAX_SERVED_UPDATE_INTERVAL_MS = 60_000L;

	@Inject
	private Logger log;

	@Inject
	private PolicyDownloadService policyDownloadService;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private TraceStore store;

	private final Map<String, CachedRelease> cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {

		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CachedRelease> eldest) {
			return size() > CACHE_MAX_ENTRIES;
		}

	});

	private Clock clock = Clock.systemUTC();

	/**
	 * Records the releases this node serves after a publication. A new release is inserted; a
	 * known one has its last-served time moved forward at most once per update interval.
	 */
	public void onPolicySnapshot(@Observes PolicySnapshotEvent event) {
		event.getCurrent().forEach((policyStoreId, source) -> {
			if (isComparable(source)) {
				try {
					recordServed(policyStoreId, source, event.getPublishedAtMs());
				} catch (RuntimeException ex) {
					log.warn("TRACE policy release not recorded: policy_store_id={}", policyStoreId, ex);
				}
			}
		});
	}

	/**
	 * @param judgedAtMs the time the record is judged at, epoch milliseconds
	 * @return {@code true} when the record names a store this node serves with a release that no
	 *         node served at or within the effective grace period before {@code judgedAtMs}, or
	 *         with a different {@code policy_store_version}
	 */
	public boolean check(PolicyRef policy, long judgedAtMs) {
		if (policy == null) {
			return false;
		}
		try {
			LoadedPolicySource source = policyDownloadService.getLoadedPolicies().get(policy.getPolicyStoreId());
			if (!isComparable(source)) {
				return false;
			}
			if (source.getBundleHash().equals(policy.getBundleHash())) {
				return versionDiffers(source.getPolicyStoreVersion(), policy);
			}
			Optional<TracePolicyReleaseEntry> release = findRelease(policy.getPolicyStoreId(), policy.getBundleHash());
			if (!release.isPresent() || release.get().getLastServedAtMs() == null
					|| release.get().getLastServedAtMs() + effectiveGraceMs() < judgedAtMs) {
				return true;
			}
			return versionDiffers(release.get().getPolicyStoreVersion(), policy);
		} catch (RuntimeException ex) {
			log.warn("TRACE policy release check skipped: policy_store_id={}", policy.getPolicyStoreId(), ex);
			return false;
		}
	}

	/**
	 * @return {@code policyMismatchGraceSeconds}, but at least two reload intervals, so a release
	 *         another node still serves never looks retired between its updates
	 */
	long effectiveGraceMs() {
		long graceSeconds = Math.max(appConfiguration.getTraceConfiguration().getPolicyMismatchGraceSeconds(),
				2L * PolicyDownloadService.RELOAD_INTERVAL_SECONDS);
		return graceSeconds * 1000L;
	}

	long servedUpdateIntervalMs() {
		return Math.min(MAX_SERVED_UPDATE_INTERVAL_MS,
				appConfiguration.getTraceConfiguration().getPolicyMismatchGraceSeconds() * 1000L / 2);
	}

	private void recordServed(String policyStoreId, LoadedPolicySource source, long servedAtMs) {
		String bundleHash = source.getBundleHash();
		String version = source.getPolicyStoreVersion();
		CachedRelease cached = cache.get(cacheKey(policyStoreId, bundleHash));
		if (cached != null && !needsUpdate(cached.release, servedAtMs)) {
			return;
		}
		Optional<TracePolicyReleaseEntry> stored = store.findPolicyRelease(policyStoreId, bundleHash);
		if (!stored.isPresent()) {
			TracePolicyReleaseEntry entry = new TracePolicyReleaseEntry();
			entry.setPolicyStoreId(policyStoreId);
			entry.setBundleHash(bundleHash);
			entry.setPolicyStoreVersion(version);
			entry.setFirstServedAtMs(servedAtMs);
			entry.setLastServedAtMs(servedAtMs);
			try {
				store.insertPolicyRelease(entry);
				remember(policyStoreId, bundleHash, entry);
				return;
			} catch (DuplicateEntryException ex) {
				stored = store.findPolicyRelease(policyStoreId, bundleHash);
			}
		}
		if (stored.isPresent()) {
			TracePolicyReleaseEntry entry = stored.get();
			if (needsUpdate(entry, servedAtMs)) {
				store.recordPolicyReleaseServed(policyStoreId, bundleHash, version, servedAtMs);
				PolicyReleaseServed.apply(entry, version, servedAtMs);
			}
			remember(policyStoreId, bundleHash, entry);
		}
	}

	private boolean needsUpdate(TracePolicyReleaseEntry release, long servedAtMs) {
		Long lastServedAtMs = release.getLastServedAtMs();
		Long firstServedAtMs = release.getFirstServedAtMs();
		return lastServedAtMs == null || servedAtMs - lastServedAtMs >= servedUpdateIntervalMs()
				|| firstServedAtMs == null || servedAtMs < firstServedAtMs;
	}

	private Optional<TracePolicyReleaseEntry> findRelease(String policyStoreId, String bundleHash) {
		String key = cacheKey(policyStoreId, bundleHash);
		long now = clock.millis();
		CachedRelease cached = cache.get(key);
		if (cached != null && now - cached.fetchedAtMs < CACHE_TTL_MS) {
			return Optional.of(cached.release);
		}
		Optional<TracePolicyReleaseEntry> release = store.findPolicyRelease(policyStoreId, bundleHash);
		release.ifPresent(found -> remember(policyStoreId, bundleHash, found));
		return release;
	}

	private void remember(String policyStoreId, String bundleHash, TracePolicyReleaseEntry release) {
		cache.put(cacheKey(policyStoreId, bundleHash), new CachedRelease(release, clock.millis()));
	}

	boolean isCached(String policyStoreId, String bundleHash) {
		return cache.containsKey(cacheKey(policyStoreId, bundleHash));
	}

	int cachedReleaseCount() {
		return cache.size();
	}

	private static String cacheKey(String policyStoreId, String bundleHash) {
		return policyStoreId + '\n' + bundleHash;
	}

	private static boolean isComparable(LoadedPolicySource source) {
		return source != null && source.isArchive() && source.getBundleHash() != null;
	}

	private static boolean versionDiffers(String servedVersion, PolicyRef policy) {
		return servedVersion != null && !servedVersion.equals(policy.getPolicyStoreVersion());
	}

	private static final class CachedRelease {

		private final TracePolicyReleaseEntry release;

		private final long fetchedAtMs;

		private CachedRelease(TracePolicyReleaseEntry release, long fetchedAtMs) {
			this.release = release;
			this.fetchedAtMs = fetchedAtMs;
		}

	}

}
