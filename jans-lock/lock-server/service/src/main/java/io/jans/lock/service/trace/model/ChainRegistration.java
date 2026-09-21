/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * A registered producer chain (design §8, T-3). Immutable and unique on {@link #getChainIdentity()}
 * (key type {@code chain}); create-only, never updated.
 *
 * @author Yuriy Movchan
 */
public final class ChainRegistration {

	private final ChainIdentity chainIdentity;

	private final long registeredAtMs;

	private final String registeredBy;

	public ChainRegistration(ChainIdentity chainIdentity, long registeredAtMs, String registeredBy) {
		this.chainIdentity = Objects.requireNonNull(chainIdentity, "chainIdentity");
		this.registeredAtMs = registeredAtMs;
		this.registeredBy = Objects.requireNonNull(registeredBy, "registeredBy");
	}

	public ChainIdentity getChainIdentity() {
		return chainIdentity;
	}

	public long getRegisteredAtMs() {
		return registeredAtMs;
	}

	public String getRegisteredBy() {
		return registeredBy;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChainRegistration)) {
			return false;
		}
		ChainRegistration other = (ChainRegistration) o;
		return chainIdentity.equals(other.chainIdentity);
	}

	@Override
	public int hashCode() {
		return chainIdentity.hashCode();
	}

	@Override
	public String toString() {
		return "ChainRegistration [chainIdentity=" + chainIdentity + ", registeredAtMs=" + registeredAtMs
				+ ", registeredBy=" + registeredBy + "]";
	}

}
