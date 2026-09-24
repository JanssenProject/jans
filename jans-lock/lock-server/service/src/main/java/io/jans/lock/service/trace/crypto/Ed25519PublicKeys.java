/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.error.TraceCryptoException;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Builds Ed25519 {@link PublicKey}s from RFC 8037 OKP JWKs
 * ({@code {"kty":"OKP","crv":"Ed25519","x":"<32 raw bytes, base64url>"}}) through the JCA
 * {@link KeyFactory} of the installed BouncyCastle provider (standard or FIPS), so no BC-internal
 * class is referenced.
 *
 * <p>The raw 32-byte key is wrapped in the RFC 8410 SubjectPublicKeyInfo DER structure
 * ({@code 30 2a 30 05 06 03 2b 65 70 03 21 00 || raw}), the only public-key encoding every JCA
 * provider is required to understand.
 *
 * <p>Not reused on purpose: {@code io.jans.as.model.crypto.AbstractCryptoProvider} and
 * {@code EDDSASigner} treat {@code x} as a full SPKI blob (non-RFC-8037) and refuse FIPS mode.
 *
 * @author Yuriy Movchan
 */
public final class Ed25519PublicKeys {

	/** JCA algorithm name used for {@link KeyFactory}, {@code Signature} and {@code KeyPairGenerator}. */
	public static final String ALGORITHM = "Ed25519";

	/** JWK key type of Edwards-curve keys (RFC 8037 §2). */
	public static final String JWK_KTY_OKP = "OKP";

	/** JWK curve name (RFC 8037 §3.1). */
	public static final String JWK_CRV_ED25519 = "Ed25519";

	/** Length of a raw Ed25519 public key (RFC 8032 §5.1.5). */
	public static final int RAW_KEY_LENGTH = 32;

	/** DER prefix of a SubjectPublicKeyInfo wrapping a 32-byte Ed25519 key (RFC 8410, OID 1.3.101.112). */
	private static final byte[] SPKI_PREFIX = new byte[] { 0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70,
			0x03, 0x21, 0x00 };

	private static final String MEMBER_KTY = "kty";
	private static final String MEMBER_CRV = "crv";
	private static final String MEMBER_X = "x";
	private static final String MEMBER_D = "d";

	private Ed25519PublicKeys() {
	}

	/**
	 * Builds the public key from a JWK given as a string map (the shape stored in
	 * {@code jansTracePublicKeyJwk}). Members other than {@code kty}, {@code crv}, {@code x} and
	 * {@code d} are ignored.
	 *
	 * @throws TraceCryptoException with reason {@code jwk_format}, {@code jwk_kty}, {@code jwk_crv},
	 *         {@code jwk_private_material}, {@code jwk_x_encoding}, {@code jwk_x_length},
	 *         {@code invalid_key} or {@code crypto_unavailable}
	 */
	public static PublicKey fromJwk(Map<String, String> jwk) {
		if (jwk == null) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_FORMAT, "JWK must not be null");
		}
		return fromJwkMembers(jwk.get(MEMBER_KTY), jwk.get(MEMBER_CRV), jwk.containsKey(MEMBER_D),
				jwk.containsKey(MEMBER_X), jwk.get(MEMBER_X));
	}

	/**
	 * Builds the public key from a JWK given as a Jackson object node (the shape received by the
	 * admin API). Members other than {@code kty}, {@code crv}, {@code x} and {@code d} are ignored.
	 *
	 * @throws TraceCryptoException see {@link #fromJwk(Map)}
	 */
	public static PublicKey fromJwk(JsonNode jwk) {
		if ((jwk == null) || !jwk.isObject()) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_FORMAT, "JWK must be a JSON object");
		}
		JsonNode x = jwk.get(MEMBER_X);
		return fromJwkMembers(textOrNull(jwk.get(MEMBER_KTY)), textOrNull(jwk.get(MEMBER_CRV)), jwk.has(MEMBER_D),
				x != null, textOrNull(x));
	}

	/**
	 * Builds the public key from its raw 32 bytes (RFC 8032 encoding, as carried in JWK {@code x}).
	 *
	 * @throws TraceCryptoException with reason {@code jwk_x_length}, {@code invalid_key} or
	 *         {@code crypto_unavailable}
	 */
	public static PublicKey fromRaw(byte[] raw) {
		if ((raw == null) || (raw.length != RAW_KEY_LENGTH)) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_X_LENGTH,
					"Ed25519 public key must be " + RAW_KEY_LENGTH + " bytes but was " + ((raw == null) ? 0 : raw.length));
		}

		byte[] spki = new byte[SPKI_PREFIX.length + RAW_KEY_LENGTH];
		System.arraycopy(SPKI_PREFIX, 0, spki, 0, SPKI_PREFIX.length);
		System.arraycopy(raw, 0, spki, SPKI_PREFIX.length, RAW_KEY_LENGTH);

		Provider provider = requireProvider();
		try {
			return KeyFactory.getInstance(ALGORITHM, provider).generatePublic(new X509EncodedKeySpec(spki));
		} catch (NoSuchAlgorithmException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
					"Provider '" + provider.getName() + "' has no " + ALGORITHM + " KeyFactory", ex);
		} catch (InvalidKeySpecException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_INVALID_KEY,
					"Provider '" + provider.getName() + "' rejected the Ed25519 public key", ex);
		}
	}

	/**
	 * @return the installed BouncyCastle provider
	 * @throws TraceCryptoException with reason {@code crypto_unavailable} if
	 *         {@code SecurityProviderUtility.installBCProvider()} has not run
	 */
	static Provider requireProvider() {
		Provider provider = SecurityProviderUtility.getBCProvider();
		if (provider == null) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
					"BouncyCastle provider is not installed");
		}
		return provider;
	}

	private static PublicKey fromJwkMembers(String kty, String crv, boolean hasD, boolean hasX, String x) {
		if (!JWK_KTY_OKP.equals(kty)) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_KTY,
					"JWK kty must be " + JWK_KTY_OKP + " but was " + describe(kty));
		}
		if (!JWK_CRV_ED25519.equals(crv)) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_CRV,
					"JWK crv must be " + JWK_CRV_ED25519 + " but was " + describe(crv));
		}
		if (hasD) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_PRIVATE_MATERIAL,
					"JWK must not carry private key material (member 'd')");
		}
		if (!hasX || (x == null)) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_X_ENCODING,
					"JWK x must be a base64url string");
		}

		byte[] raw;
		try {
			raw = StrictBase64Url.decode(x);
		} catch (IllegalArgumentException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_X_ENCODING,
					"JWK x is not strict unpadded base64url: " + ex.getMessage(), ex);
		}
		return fromRaw(raw);
	}

	private static String textOrNull(JsonNode node) {
		return ((node != null) && node.isTextual()) ? node.textValue() : null;
	}

	private static String describe(String value) {
		return (value == null) ? "absent" : ("'" + value + "'");
	}

}
