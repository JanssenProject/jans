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
import io.jans.lock.model.trace.config.TraceConfiguration;
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
 * served it within the acceptance window before the time the record is judged at. A store
 * this node does not serve is never a mismatch, because producers may load stores from
 * elsewhere. The check is best-effort: a failure never rejects or flags a record.
 */
@ApplicationScoped
public class PolicyMismatchCheck {

	static final long CACHE_TTL_MS = 10_000L;

	static final long NEGATIVE_CACHE_TTL_MS = 1_000L;

	static final int PRUNE_BATCH_LIMIT = 500;

	static final int CACHE_MAX_ENTRIES = 1_000;

	static final long SERVED_UPDATE_INTERVAL_MS = 60_000L;

	/**
	 * How far a stored last-served time can trail the moment a node last served the release: one
	 * update interval, plus a reload interval before the node notices and one more before it
	 * publishes, plus the cache lifetime on the judging node.
	 */
	static final long LAST_SERVED_STALENESS_MS = SERVED_UPDATE_INTERVAL_MS
			+ 2_000L * PolicyDownloadService.RELOAD_INTERVAL_SECONDS + CACHE_TTL_MS;

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
		synchronized (cache) {
			cache.values().removeIf(cached -> cached.release == null);
		}
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
	 *         node served at or within the acceptance window before {@code judgedAtMs}, or
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
					|| release.get().getLastServedAtMs() + acceptanceWindowMs() < judgedAtMs) {
				return true;
			}
			return versionDiffers(release.get().getPolicyStoreVersion(), policy);
		} catch (RuntimeException ex) {
			log.warn("TRACE policy release check skipped: policy_store_id={}", policy.getPolicyStoreId(), ex);
			return false;
		}
	}

	/**
	 * @return how long after its stored last-served time a release still matches: the larger of
	 *         {@code policyMismatchGraceSeconds} and {@code latenessThresholdSeconds} (a record
	 *         judged at receipt may arrive that much after it was signed), plus
	 *         {@link #LAST_SERVED_STALENESS_MS}
	 */
	long acceptanceWindowMs() {
		TraceConfiguration config = appConfiguration.getTraceConfiguration();
		long seconds = Math.max(config.getPolicyMismatchGraceSeconds(), config.getLatenessThresholdSeconds());
		return seconds * 1000L + LAST_SERVED_STALENESS_MS;
	}

	private void recordServed(String policyStoreId, LoadedPolicySource source, long servedAtMs) {
		String bundleHash = source.getBundleHash();
		String version = source.getPolicyStoreVersion();
		CachedRelease cached = cache.get(cacheKey(policyStoreId, bundleHash));
		if (cached != null && cached.release != null && !needsUpdate(cached.release, servedAtMs)) {
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

	/**
	 * Deletes up to {@value #PRUNE_BATCH_LIMIT} releases no node has served for
	 * {@code policyReleaseRetentionDays} (never less than the acceptance window), skipping any
	 * release this node serves now. Each delete is conditional on the release still being that
	 * old, so one another node refreshed meanwhile is kept, and one another node already deleted
	 * is not an error. A failed delete is skipped and the rest of the batch still runs.
	 *
	 * @return how many releases this call deleted
	 */
	public int pruneExpiredReleases() {
		long retentionMs = Math.max(
				appConfiguration.getTraceConfiguration().getPolicyReleaseRetentionDays() * 86_400_000L,
				acceptanceWindowMs());
		long cutoffMs = clock.millis() - retentionMs;
		Map<String, LoadedPolicySource> served = policyDownloadService.getLoadedPolicies();
		int removed = 0;
		int failed = 0;
		RuntimeException firstFailure = null;
		for (TracePolicyReleaseEntry release : store.findPolicyReleasesLastServedBefore(cutoffMs, PRUNE_BATCH_LIMIT)) {
			LoadedPolicySource source = served.get(release.getPolicyStoreId());
			if (isComparable(source) && source.getBundleHash().equals(release.getBundleHash())) {
				continue;
			}
			try {
				if (store.removePolicyReleaseLastServedBefore(release.getPolicyStoreId(), release.getBundleHash(),
						cutoffMs)) {
					removed++;
				}
				cache.remove(cacheKey(release.getPolicyStoreId(), release.getBundleHash()));
			} catch (RuntimeException ex) {
				failed++;
				if (firstFailure == null) {
					firstFailure = ex;
				}
			}
		}
		if (failed > 0) {
			log.warn("TRACE policy release retention: {} deletes failed", failed, firstFailure);
		}
		return removed;
	}

	private boolean needsUpdate(TracePolicyReleaseEntry release, long servedAtMs) {
		Long lastServedAtMs = release.getLastServedAtMs();
		Long firstServedAtMs = release.getFirstServedAtMs();
		return lastServedAtMs == null || servedAtMs - lastServedAtMs >= SERVED_UPDATE_INTERVAL_MS
				|| firstServedAtMs == null || servedAtMs < firstServedAtMs;
	}

	private Optional<TracePolicyReleaseEntry> findRelease(String policyStoreId, String bundleHash) {
		String key = cacheKey(policyStoreId, bundleHash);
		long now = clock.millis();
		CachedRelease cached = cache.get(key);
		if (cached != null
				&& now - cached.fetchedAtMs < (cached.release == null ? NEGATIVE_CACHE_TTL_MS : CACHE_TTL_MS)) {
			return Optional.ofNullable(cached.release);
		}
		Optional<TracePolicyReleaseEntry> release = store.findPolicyRelease(policyStoreId, bundleHash);
		remember(policyStoreId, bundleHash, release.orElse(null));
		return release;
	}

	private void remember(String policyStoreId, String bundleHash, TracePolicyReleaseEntry release) {
		cache.put(cacheKey(policyStoreId, bundleHash), new CachedRelease(release, clock.millis()));
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
