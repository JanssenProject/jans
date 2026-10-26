/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.core.cedarling.service.policy;

import java.util.Collections;
import java.util.Map;

import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;

/**
 * Fired synchronously by {@link PolicyDownloadService} each time it publishes a new set of loaded
 * policy stores, including the first load and a reset to none.
 */
public final class PolicySnapshotEvent {

	private final Map<String, LoadedPolicySource> previous;

	private final Map<String, LoadedPolicySource> current;

	private final long publishedAtMs;

	public PolicySnapshotEvent(Map<String, LoadedPolicySource> previous, Map<String, LoadedPolicySource> current,
			long publishedAtMs) {
		this.previous = Collections.unmodifiableMap(previous);
		this.current = Collections.unmodifiableMap(current);
		this.publishedAtMs = publishedAtMs;
	}

	/** @return the stores served until this publication, keyed by policy store id */
	public Map<String, LoadedPolicySource> getPrevious() {
		return previous;
	}

	/** @return the stores served from this publication on, keyed by policy store id */
	public Map<String, LoadedPolicySource> getCurrent() {
		return current;
	}

	/** @return when the new set replaced the previous one, in epoch milliseconds */
	public long getPublishedAtMs() {
		return publishedAtMs;
	}

}
