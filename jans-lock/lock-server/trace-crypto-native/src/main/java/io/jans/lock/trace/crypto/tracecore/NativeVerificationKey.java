/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import io.jans.lock.trace.crypto.TraceVerificationKey;
import io.jans.trace.PublicKey;

/**
 * A key parsed by {@link NativeTraceCryptoProvider}: the {@code jans-trace-core} key plus its
 * RFC 7638 thumbprint.
 *
 * @author Yuriy Movchan
 */
final class NativeVerificationKey implements TraceVerificationKey {

	final PublicKey key;

	private final String thumbprint;

	NativeVerificationKey(PublicKey key, String thumbprint) {
		this.key = key;
		this.thumbprint = thumbprint;
	}

	@Override
	public String thumbprint() {
		return thumbprint;
	}

}
