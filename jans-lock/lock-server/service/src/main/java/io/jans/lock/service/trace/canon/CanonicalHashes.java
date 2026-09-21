/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.canon;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * SHA-256 digests in the TRACE {@code sha256:<64 lowercase hex>} format over raw bytes or over
 * the JCS canonical form of a JSON value.
 *
 * <p>Uses {@link MessageDigest#getInstance(String)} with the JDK provider so it behaves the same
 * in the standard and the FIPS (BouncyCastle FIPS) WAR variants.
 * 
 * @author Yuriy Movchan
 */
public final class CanonicalHashes {

	/** Prefix of every TRACE hash string. */
	public static final String SHA256_PREFIX = "sha256:";

	private static final String SHA256_ALGORITHM = "SHA-256";

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private CanonicalHashes() {
	}

	/**
	 * @return {@code "sha256:" + lowercaseHex(SHA-256(input))}
	 */
	public static String sha256Prefixed(byte[] input) {
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}
		return SHA256_PREFIX + toLowerHex(sha256().digest(input));
	}

	/**
	 * @return {@code "sha256:" + lowercaseHex(SHA-256(UTF-8(JCS(node))))}
	 * @throws JcsException if the node cannot be canonicalized
	 */
	public static String sha256PrefixedOfJcs(JsonNode node) {
		return sha256Prefixed(JcsCanonicalizer.canonicalizeToUtf8(node));
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance(SHA256_ALGORITHM);
		} catch (NoSuchAlgorithmException ex) {
			// Every Java platform is required to support SHA-256.
			throw new IllegalStateException("SHA-256 MessageDigest is not available", ex);
		}
	}

	private static String toLowerHex(byte[] bytes) {
		char[] out = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int b = bytes[i] & 0xFF;
			out[2 * i] = HEX[b >>> 4];
			out[2 * i + 1] = HEX[b & 0xF];
		}
		return new String(out);
	}

}
