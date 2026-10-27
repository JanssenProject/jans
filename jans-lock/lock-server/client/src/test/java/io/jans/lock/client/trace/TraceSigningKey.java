/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client.trace;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Test-only Ed25519 key pair generator/signer for TRACE assertions, used only by
 * {@code TraceEndToEndTest} to produce records the running Lock Server verifies. Java 11 has no
 * built-in EdDSA provider (added upstream in JDK 15), so this installs BouncyCastle the same way
 * the service module's test kit does (see {@code io.jans.lock.service.trace.testkit.TraceTestKeys},
 * not reused here because the client module does not depend on the service module).
 */
public final class TraceSigningKey {

	private static final String ALGORITHM = "Ed25519";

	private static final int SPKI_LENGTH = 44;

	private static final int RAW_KEY_LENGTH = 32;

	private static final Base64.Encoder BASE64URL_NO_PAD = Base64.getUrlEncoder().withoutPadding();

	private static volatile boolean providerInstalled;

	private final KeyPair keyPair;

	private TraceSigningKey(KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	public static TraceSigningKey generate() {
		installProviderOnce();
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
			return new TraceSigningKey(generator.generateKeyPair());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to generate Ed25519 key pair", ex);
		}
	}

	private static void installProviderOnce() {
		if (!providerInstalled) {
			synchronized (TraceSigningKey.class) {
				if (!providerInstalled) {
					if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
						Security.addProvider(new BouncyCastleProvider());
					}
					providerInstalled = true;
				}
			}
		}
	}

	/** @return a raw Ed25519 signature (64 bytes) over {@code message} */
	public byte[] sign(byte[] message) {
		try {
			Signature signer = Signature.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
			signer.initSign(keyPair.getPrivate());
			signer.update(message);
			return signer.sign();
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to sign with Ed25519 key", ex);
		}
	}

	/** @return {@code base64url(sign(message))}, unpadded, matching the TRACE wire format */
	public String signToBase64Url(byte[] message) {
		return BASE64URL_NO_PAD.encodeToString(sign(message));
	}

	/** @return RFC 8037 {@code {"kty":"OKP","crv":"Ed25519","x":"<raw 32-byte key, base64url>"}} */
	public Map<String, String> publicJwk() {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", BASE64URL_NO_PAD.encodeToString(rawPublicKey(keyPair.getPublic())));
		return jwk;
	}

	private static byte[] rawPublicKey(PublicKey publicKey) {
		byte[] encoded = publicKey.getEncoded();
		if ((encoded == null) || (encoded.length != SPKI_LENGTH)) {
			throw new IllegalStateException("Unexpected Ed25519 SubjectPublicKeyInfo length: "
					+ ((encoded == null) ? "null" : encoded.length));
		}
		byte[] raw = new byte[RAW_KEY_LENGTH];
		System.arraycopy(encoded, SPKI_LENGTH - RAW_KEY_LENGTH, raw, 0, RAW_KEY_LENGTH);
		return raw;
	}

}
