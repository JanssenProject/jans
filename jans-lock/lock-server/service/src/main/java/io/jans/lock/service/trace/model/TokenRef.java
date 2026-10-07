/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * A token reference extracted from a {@code trace.event.tokens[]} entry (design §7.3). The
 * external token identity is {@code (evidence_domain_id, issuer, jti)}, or
 * {@code (evidence_domain_id, issuer, fingerprint)} when no {@code jti} is present; exactly one
 * of {@link #getJti()} / {@link #getFingerprint()} is non-{@code null}. Raw bearer tokens are
 * never carried here, only the reference.
 *
 * <p>Placed in {@code io.jans.lock.service.trace.model} (rather than {@code validate}) so the
 * store layer (task 12) can reuse this class directly.
 *
 * @author Yuriy Movchan
 */
public final class TokenRef {

	private final String issuer;

	private final String tokenType;

	private final String jti;

	private final String fingerprint;

	public TokenRef(String issuer, String tokenType, String jti, String fingerprint) {
		this.issuer = issuer;
		this.tokenType = tokenType;
		this.jti = jti;
		this.fingerprint = fingerprint;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getTokenType() {
		return tokenType;
	}

	/**
	 * @return the token's {@code jti}, or {@code null} when this reference identifies the token by
	 *         {@link #getFingerprint()} instead
	 */
	public String getJti() {
		return jti;
	}

	/**
	 * @return the token's {@code fingerprint}, or {@code null} when this reference identifies the
	 *         token by {@link #getJti()} instead
	 */
	public String getFingerprint() {
		return fingerprint;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TokenRef)) {
			return false;
		}
		TokenRef other = (TokenRef) o;
		return Objects.equals(issuer, other.issuer) && Objects.equals(tokenType, other.tokenType)
				&& Objects.equals(jti, other.jti) && Objects.equals(fingerprint, other.fingerprint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(issuer, tokenType, jti, fingerprint);
	}

	@Override
	public String toString() {
		return "TokenRef [issuer=" + issuer + ", tokenType=" + tokenType + ", jti=" + jti + ", fingerprint="
				+ fingerprint + "]";
	}

}
