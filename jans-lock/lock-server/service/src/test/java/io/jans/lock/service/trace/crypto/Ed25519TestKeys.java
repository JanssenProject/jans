/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.Map;

import io.jans.lock.service.trace.testkit.TraceTestKeys;

/**
 * Delegates to {@link TraceTestKeys} (task 22 consolidated test kit, moved to the
 * {@code testkit} package). Kept only so call sites from tasks 07/09/16/19 keep compiling.
 *
 * @deprecated use {@link TraceTestKeys} in new code.
 */
@Deprecated
public final class Ed25519TestKeys {

	private Ed25519TestKeys() {
	}

	public static void installProvider() {
		TraceTestKeys.installProvider();
	}

	public static Provider provider() {
		return TraceTestKeys.provider();
	}

	public static KeyPair generateKeyPair() {
		return TraceTestKeys.generateKeyPair();
	}

	public static PrivateKey privateKeyFromSeed(byte[] seed) {
		return TraceTestKeys.privateKeyFromSeed(seed);
	}

	public static PublicKey publicKeyFromRaw(byte[] raw) {
		return TraceTestKeys.publicKeyFromRaw(raw);
	}

	public static byte[] sign(PrivateKey privateKey, byte[] message) {
		return TraceTestKeys.sign(privateKey, message);
	}

	public static byte[] rawPublicKey(PublicKey publicKey) {
		return TraceTestKeys.rawPublicKey(publicKey);
	}

	public static Map<String, String> toJwk(PublicKey publicKey) {
		return TraceTestKeys.toJwk(publicKey);
	}

	public static byte[] hex(String hex) {
		return TraceTestKeys.hex(hex);
	}

}
