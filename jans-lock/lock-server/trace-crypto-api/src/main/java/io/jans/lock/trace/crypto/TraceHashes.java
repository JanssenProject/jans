/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provider-neutral hash constants and helpers shared by every {@link TraceCryptoProvider}
 * implementation and by the service. Uses the JDK {@link MessageDigest}, so it behaves the same
 * in the standard and the FIPS WAR variants.
 *
 * @author Yuriy Movchan
 */
public final class TraceHashes {

	/** Prefix of every TRACE hash string. */
	public static final String SHA256_PREFIX = "sha256:";

	/** The genesis predecessor sentinel: {@code "sha256:"} followed by 64 zero digits. */
	public static final String ZERO_HASH = SHA256_PREFIX + "0".repeat(64);

	/**
	 * Receipt-commitment profile tag (GovOps profile §11). It is the {@code receipt_profile} member
	 * of every receipt-hash input; a different receipt-hash layout needs a new tag.
	 */
	public static final String RECEIPT_PROFILE = "tag:jans.io,2026:lock-trace-receipt-v1";

	private static final String SHA256_ALGORITHM = "SHA-256";

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private TraceHashes() {
	}

	/**
	 * @return {@code "sha256:" + lowercaseHex(SHA-256(input))}
	 * @throws IllegalArgumentException if {@code input} is null
	 */
	public static String sha256Prefixed(byte[] input) {
		return SHA256_PREFIX + sha256Hex(input);
	}

	/**
	 * @return {@code lowercaseHex(SHA-256(input))} without prefix, for storage keys
	 * @throws IllegalArgumentException if {@code input} is null
	 */
	public static String sha256Hex(byte[] input) {
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}
		byte[] digest;
		try {
			digest = MessageDigest.getInstance(SHA256_ALGORITHM).digest(input);
		} catch (NoSuchAlgorithmException ex) {
			// Every Java platform is required to support SHA-256.
			throw new IllegalStateException("SHA-256 MessageDigest is not available", ex);
		}
		char[] out = new char[digest.length * 2];
		for (int i = 0; i < digest.length; i++) {
			int b = digest[i] & 0xFF;
			out[2 * i] = HEX[b >>> 4];
			out[2 * i + 1] = HEX[b & 0xF];
		}
		return new String(out);
	}

}
