/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * Signature verification outcome carried by the store layer (design §10, T-1). This is a plain
 * value copy independent of the {@code TraceVerification} ORM POJO (task 03) — the store
 * abstraction never depends on ORM/JSON-column types.
 *
 * @author Yuriy Movchan
 */
public final class VerificationResult {

	private final boolean signatureValid;

	private final String keyId;

	private final long verifiedAtMs;

	private final String algorithm;

	private final String keyThumbprint;

	private final Boolean keyAuthorized;

	/**
	 * Builds a result without the key thumbprint and authorization summary: the shape of rows
	 * stored before those members existed.
	 */
	public VerificationResult(boolean signatureValid, String keyId, long verifiedAtMs, String algorithm) {
		this(signatureValid, keyId, verifiedAtMs, algorithm, null, null);
	}

	/**
	 * @param keyThumbprint {@code sha256:<hex>} RFC 7638 thumbprint of the key used, or
	 *        {@code null} for a row stored before the thumbprint was recorded
	 * @param keyAuthorized the admission summary, or {@code null} for a row stored before it was
	 *        recorded
	 */
	public VerificationResult(boolean signatureValid, String keyId, long verifiedAtMs, String algorithm,
			String keyThumbprint, Boolean keyAuthorized) {
		this.keyThumbprint = keyThumbprint;
		this.keyAuthorized = keyAuthorized;
		this.signatureValid = signatureValid;
		this.keyId = Objects.requireNonNull(keyId, "keyId");
		this.verifiedAtMs = verifiedAtMs;
		this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
	}

	public boolean isSignatureValid() {
		return signatureValid;
	}

	public String getKeyId() {
		return keyId;
	}

	public long getVerifiedAtMs() {
		return verifiedAtMs;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * @return the thumbprint of the key the record was verified against, or {@code null} for a
	 *         legacy row
	 */
	public String getKeyThumbprint() {
		return keyThumbprint;
	}

	/**
	 * @return whether the key was admitted for this record, or {@code null} for a legacy row
	 */
	public Boolean getKeyAuthorized() {
		return keyAuthorized;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VerificationResult)) {
			return false;
		}
		VerificationResult other = (VerificationResult) o;
		return signatureValid == other.signatureValid && verifiedAtMs == other.verifiedAtMs
				&& keyId.equals(other.keyId) && algorithm.equals(other.algorithm)
				&& Objects.equals(keyThumbprint, other.keyThumbprint) && Objects.equals(keyAuthorized, other.keyAuthorized);
	}

	@Override
	public int hashCode() {
		return Objects.hash(signatureValid, keyId, verifiedAtMs, algorithm, keyThumbprint, keyAuthorized);
	}

	@Override
	public String toString() {
		return "VerificationResult [signatureValid=" + signatureValid + ", keyId=" + keyId + ", verifiedAtMs="
				+ verifiedAtMs + ", algorithm=" + algorithm + ", keyThumbprint=" + keyThumbprint + ", keyAuthorized="
				+ keyAuthorized + "]";
	}

}
