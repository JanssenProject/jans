/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

/**
 * A verification key built by {@link TraceCryptoProvider#parseKey(java.util.Map)}. Opaque to
 * callers: only the provider that created it can use it in
 * {@link TraceCryptoProvider#verifyAndDigest}.
 *
 * @author Yuriy Movchan
 */
public interface TraceVerificationKey {

	/**
	 * @return the RFC 7638 JWK thumbprint of the key in {@code sha256:<64 lowercase hex>} form
	 */
	String thumbprint();

}
