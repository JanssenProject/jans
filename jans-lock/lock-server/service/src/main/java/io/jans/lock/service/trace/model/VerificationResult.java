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

	public VerificationResult(boolean signatureValid, String keyId, long verifiedAtMs, String algorithm) {
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
				&& keyId.equals(other.keyId) && algorithm.equals(other.algorithm);
	}

	@Override
	public int hashCode() {
		return Objects.hash(signatureValid, keyId, verifiedAtMs, algorithm);
	}

	@Override
	public String toString() {
		return "VerificationResult [signatureValid=" + signatureValid + ", keyId=" + keyId + ", verifiedAtMs="
				+ verifiedAtMs + ", algorithm=" + algorithm + "]";
	}

}
