/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;

/**
 * Per-node memory of the policy store archive releases Lock has served, keyed by
 * {@code policy_store_id} and {@code bundle_hash}. A release still served is current; one that
 * stopped being served keeps the time Lock first saw it gone. The history starts empty on each
 * node start and keeps at most {@value #MAX_RELEASES_PER_STORE} releases per store.
 */
final class PolicyReleaseHistory {

	static final int MAX_RELEASES_PER_STORE = 50;

	private final Map<String, LinkedHashMap<String, ServedRelease>> releases = new HashMap<>();

	/** Records the releases served now and retires those no longer served. */
	synchronized void observe(Map<String, LoadedPolicySource> served, long nowMs) {
		served.forEach((storeId, source) -> {
			if (source != null && source.isArchive() && source.getBundleHash() != null) {
				LinkedHashMap<String, ServedRelease> store = releases.computeIfAbsent(storeId,
						k -> new LinkedHashMap<>());
				store.remove(source.getBundleHash());
				store.put(source.getBundleHash(), ServedRelease.current(source.getPolicyStoreVersion()));
				trim(store);
			}
		});
		releases.forEach((storeId, store) -> {
			String currentHash = currentHash(served.get(storeId));
			store.replaceAll((hash, release) -> release.isCurrent() && !hash.equals(currentHash)
					? release.retiredAt(nowMs)
					: release);
		});
	}

	synchronized Optional<ServedRelease> find(String policyStoreId, String bundleHash) {
		Map<String, ServedRelease> store = releases.get(policyStoreId);
		return store == null ? Optional.empty() : Optional.ofNullable(store.get(bundleHash));
	}

	private static String currentHash(LoadedPolicySource source) {
		return source != null && source.isArchive() ? source.getBundleHash() : null;
	}

	private static void trim(LinkedHashMap<String, ServedRelease> store) {
		Iterator<String> eldest = store.keySet().iterator();
		while (store.size() > MAX_RELEASES_PER_STORE && eldest.hasNext()) {
			eldest.next();
			eldest.remove();
		}
	}

	/** A served release: its {@code policy_store_version}, and when it stopped being served. */
	static final class ServedRelease {

		private static final long CURRENT = Long.MAX_VALUE;

		private final String version;

		private final long retiredAtMs;

		private ServedRelease(String version, long retiredAtMs) {
			this.version = version;
			this.retiredAtMs = retiredAtMs;
		}

		static ServedRelease current(String version) {
			return new ServedRelease(version, CURRENT);
		}

		ServedRelease retiredAt(long nowMs) {
			return new ServedRelease(version, nowMs);
		}

		boolean isCurrent() {
			return retiredAtMs == CURRENT;
		}

		String getVersion() {
			return version;
		}

		long getRetiredAtMs() {
			return retiredAtMs;
		}

	}

}
