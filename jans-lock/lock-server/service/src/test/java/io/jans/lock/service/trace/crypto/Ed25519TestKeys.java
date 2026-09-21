/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import io.jans.util.security.SecurityProviderUtility;

/**
 * Test-only Ed25519 helpers shared by the TRACE crypto, verification and acceptance tests:
 * provider installation, key generation, deterministic keys from RFC 8032 seeds, signing, and
 * RFC 8037 JWK export of public keys.
 */
public final class Ed25519TestKeys {

	/** PKCS#8 prefix of an Ed25519 private key holding a 32-byte seed (RFC 8410 §7). */
	private static final byte[] PKCS8_PREFIX = hex("302e020100300506032b657004220420");

	private static final int SPKI_LENGTH = 44;

	private Ed25519TestKeys() {
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
			return KeyPairGenerator.getInstance(Ed25519PublicKeys.ALGORITHM, provider()).generateKeyPair();
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
			return KeyFactory.getInstance(Ed25519PublicKeys.ALGORITHM, provider())
					.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static PublicKey publicKeyFromRaw(byte[] raw) {
		installProvider();
		return Ed25519PublicKeys.fromRaw(raw);
	}

	public static byte[] sign(PrivateKey privateKey, byte[] message) {
		try {
			Signature signer = Signature.getInstance(Ed25519PublicKeys.ALGORITHM, provider());
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
		byte[] raw = new byte[Ed25519PublicKeys.RAW_KEY_LENGTH];
		System.arraycopy(encoded, SPKI_LENGTH - raw.length, raw, 0, raw.length);
		return raw;
	}

	/**
	 * @return {@code {"kty":"OKP","crv":"Ed25519","x":"<raw key, base64url>"}} (RFC 8037 §2)
	 */
	public static Map<String, String> toJwk(PublicKey publicKey) {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", Ed25519PublicKeys.JWK_KTY_OKP);
		jwk.put("crv", Ed25519PublicKeys.JWK_CRV_ED25519);
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
