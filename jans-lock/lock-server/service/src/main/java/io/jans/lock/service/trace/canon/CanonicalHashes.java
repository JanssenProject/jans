/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.canon;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.trace.crypto.TraceHashes;

/**
 * SHA-256 digests in the TRACE {@code sha256:<64 lowercase hex>} format over raw bytes or over
 * the JCS canonical form of a JSON value.
 *
 * <p>Delegates to {@link TraceHashes}, which uses the JDK provider so it behaves the same
 * in the standard and the FIPS (BouncyCastle FIPS) WAR variants.
 * 
 * @author Yuriy Movchan
 */
public final class CanonicalHashes {

	/** Prefix of every TRACE hash string. */
	public static final String SHA256_PREFIX = TraceHashes.SHA256_PREFIX;

	private CanonicalHashes() {
	}

	/**
	 * @return {@code "sha256:" + lowercaseHex(SHA-256(input))}
	 */
	public static String sha256Prefixed(byte[] input) {
		return TraceHashes.sha256Prefixed(input);
	}

	/**
	 * @return {@code "sha256:" + lowercaseHex(SHA-256(UTF-8(JCS(node))))}
	 * @throws JcsException if the node cannot be canonicalized
	 */
	public static String sha256PrefixedOfJcs(JsonNode node) {
		return sha256Prefixed(JcsCanonicalizer.canonicalizeToUtf8(node));
	}

}
