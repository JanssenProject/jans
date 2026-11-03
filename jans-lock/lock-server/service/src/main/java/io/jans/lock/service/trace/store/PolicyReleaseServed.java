/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;

/**
 * The served-time rule shared by every {@link TraceStore}: the last-served time never moves back,
 * the first-served time never moves forward.
 */
public final class PolicyReleaseServed {

	private PolicyReleaseServed() {
	}

	/**
	 * Applies a sighting at {@code servedAtMs} to {@code entry}.
	 *
	 * @return {@code true} when {@code entry} changed and must be written
	 */
	public static boolean apply(TracePolicyReleaseEntry entry, String policyStoreVersion, long servedAtMs) {
		boolean changed = false;
		Long lastServedAtMs = entry.getLastServedAtMs();
		if (lastServedAtMs == null || servedAtMs > lastServedAtMs) {
			entry.setLastServedAtMs(servedAtMs);
			entry.setPolicyStoreVersion(policyStoreVersion);
			changed = true;
		}
		Long firstServedAtMs = entry.getFirstServedAtMs();
		if (firstServedAtMs == null || servedAtMs < firstServedAtMs) {
			entry.setFirstServedAtMs(servedAtMs);
			changed = true;
		}
		return changed;
	}

}
