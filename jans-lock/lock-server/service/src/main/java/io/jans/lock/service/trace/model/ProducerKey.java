/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A registered producer Ed25519 key (design §6, T-4, design decision D-4). Unique on
 * {@code (domainId, producerId, kid)} (key type {@code pkey}); create-only — only
 * {@link #getRevokedAtMs()} may change, via {@link #withRevokedAt(long)}.
 *
 * @author Yuriy Movchan
 */
public final class ProducerKey {

	private final String domainId;

	private final String producerId;

	private final String kid;

	private final Map<String, String> publicKeyJwk;

	private final long validFromMs;

	private final Long validUntilMs;

	private final Long revokedAtMs;

	private final String registeredBy;

	private final long createdAtMs;

	public ProducerKey(String domainId, String producerId, String kid, Map<String, String> publicKeyJwk,
			long validFromMs, Long validUntilMs, Long revokedAtMs, String registeredBy, long createdAtMs) {
		this.domainId = Objects.requireNonNull(domainId, "domainId");
		this.producerId = Objects.requireNonNull(producerId, "producerId");
		this.kid = Objects.requireNonNull(kid, "kid");
		this.publicKeyJwk = Collections
				.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(publicKeyJwk, "publicKeyJwk")));
		this.validFromMs = validFromMs;
		this.validUntilMs = validUntilMs;
		this.revokedAtMs = revokedAtMs;
		this.registeredBy = Objects.requireNonNull(registeredBy, "registeredBy");
		this.createdAtMs = createdAtMs;
	}

	public String getDomainId() {
		return domainId;
	}

	public String getProducerId() {
		return producerId;
	}

	public String getKid() {
		return kid;
	}

	/**
	 * @return an unmodifiable map, never {@code null}
	 */
	public Map<String, String> getPublicKeyJwk() {
		return publicKeyJwk;
	}

	public long getValidFromMs() {
		return validFromMs;
	}

	/**
	 * @return the expiry in epoch milliseconds, or {@code null} for no expiry
	 */
	public Long getValidUntilMs() {
		return validUntilMs;
	}

	/**
	 * @return the revocation time in epoch milliseconds, or {@code null} if not revoked
	 */
	public Long getRevokedAtMs() {
		return revokedAtMs;
	}

	public String getRegisteredBy() {
		return registeredBy;
	}

	/**
	 * @return the epoch-millisecond instant this key was registered
	 */
	public long getCreatedAtMs() {
		return createdAtMs;
	}

	/**
	 * @return an independent copy of this key with {@link #getRevokedAtMs()} replaced
	 */
	public ProducerKey withRevokedAt(long newRevokedAtMs) {
		return new ProducerKey(domainId, producerId, kid, publicKeyJwk, validFromMs, validUntilMs, newRevokedAtMs,
				registeredBy, createdAtMs);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ProducerKey)) {
			return false;
		}
		ProducerKey other = (ProducerKey) o;
		return domainId.equals(other.domainId) && producerId.equals(other.producerId) && kid.equals(other.kid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(domainId, producerId, kid);
	}

	@Override
	public String toString() {
		return "ProducerKey [domainId=" + domainId + ", producerId=" + producerId + ", kid=" + kid + ", validFromMs="
				+ validFromMs + ", validUntilMs=" + validUntilMs + ", revokedAtMs=" + revokedAtMs + ", registeredBy="
				+ registeredBy + ", createdAtMs=" + createdAtMs + "]";
	}

}
