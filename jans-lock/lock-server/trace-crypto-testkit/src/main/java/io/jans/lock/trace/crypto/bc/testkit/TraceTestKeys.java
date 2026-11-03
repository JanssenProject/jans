/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.bc.testkit;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Test-only Ed25519 helpers shared by the whole TRACE test suite: provider installation, key
 * generation, deterministic keys from RFC 8032 seeds, signing, and RFC 8037 JWK export of public
 * keys (consolidated test kit).
 */
public final class TraceTestKeys {

	/** PKCS#8 prefix of an Ed25519 private key holding a 32-byte seed (RFC 8410 §7). */
	private static final byte[] PKCS8_PREFIX = hex("302e020100300506032b657004220420");

	/** SubjectPublicKeyInfo prefix of a raw 32-byte Ed25519 public key (RFC 8410 §4). */
	private static final byte[] SPKI_PREFIX = hex("302a300506032b6570032100");

	private static final String ALGORITHM = "Ed25519";

	private static final int RAW_KEY_LENGTH = 32;

	private static final int SPKI_LENGTH = 44;

	private TraceTestKeys() {
	}

	/**
	 * Installs the BouncyCastle provider through {@link SecurityProviderUtility} unless a previous
	 * test class already did. Call from {@code @BeforeAll}.
	 */
	public static void installProvider() {
		if (SecurityProviderUtility.getBCProvider() == null) {
			SecurityProviderUtility.installBCProvider(true);
		}
	}

	public static Provider provider() {
		installProvider();
		return SecurityProviderUtility.getBCProvider();
	}

	public static KeyPair generateKeyPair() {
		try {
			return KeyPairGenerator.getInstance(ALGORITHM, provider()).generateKeyPair();
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @param seed the 32-byte RFC 8032 secret key (seed)
	 */
	public static PrivateKey privateKeyFromSeed(byte[] seed) {
		if ((seed == null) || (seed.length != 32)) {
			throw new IllegalArgumentException("seed must be 32 bytes");
		}
		byte[] pkcs8 = new byte[PKCS8_PREFIX.length + seed.length];
		System.arraycopy(PKCS8_PREFIX, 0, pkcs8, 0, PKCS8_PREFIX.length);
		System.arraycopy(seed, 0, pkcs8, PKCS8_PREFIX.length, seed.length);
		try {
			return KeyFactory.getInstance(ALGORITHM, provider())
					.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static PublicKey publicKeyFromRaw(byte[] raw) {
		if ((raw == null) || (raw.length != RAW_KEY_LENGTH)) {
			throw new IllegalArgumentException("raw key must be " + RAW_KEY_LENGTH + " bytes");
		}
		byte[] spki = new byte[SPKI_PREFIX.length + raw.length];
		System.arraycopy(SPKI_PREFIX, 0, spki, 0, SPKI_PREFIX.length);
		System.arraycopy(raw, 0, spki, SPKI_PREFIX.length, raw.length);
		try {
			return KeyFactory.getInstance(ALGORITHM, provider()).generatePublic(new X509EncodedKeySpec(spki));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static byte[] sign(PrivateKey privateKey, byte[] message) {
		try {
			Signature signer = Signature.getInstance(ALGORITHM, provider());
			signer.initSign(privateKey);
			signer.update(message);
			return signer.sign();
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @return the raw 32-byte key extracted from the provider's SubjectPublicKeyInfo encoding
	 */
	public static byte[] rawPublicKey(PublicKey publicKey) {
		byte[] encoded = publicKey.getEncoded();
		if ((encoded == null) || (encoded.length != SPKI_LENGTH)) {
			throw new IllegalStateException("unexpected SPKI length");
		}
		byte[] raw = new byte[RAW_KEY_LENGTH];
		System.arraycopy(encoded, SPKI_LENGTH - raw.length, raw, 0, raw.length);
		return raw;
	}

	/**
	 * @return {@code {"kty":"OKP","crv":"Ed25519","x":"<raw key, base64url>"}} (RFC 8037 §2)
	 */
	public static Map<String, String> toJwk(PublicKey publicKey) {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", ALGORITHM);
		jwk.put("x", StrictBase64Url.encode(rawPublicKey(publicKey)));
		return jwk;
	}

	public static byte[] hex(String hex) {
		try {
			return Hex.decodeHex(hex);
		} catch (DecoderException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}
