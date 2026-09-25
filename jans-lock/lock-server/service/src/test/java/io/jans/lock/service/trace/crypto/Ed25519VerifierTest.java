/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * RFC 8032 §7.1 and RFC 8037 Appendix A.4 conformance tests for {@link Ed25519Verifier}.
 */
class Ed25519VerifierTest {

	private static final Ed25519Verifier VERIFIER = new Ed25519Verifier();

	/** RFC 8037 Appendix A.4: JWS signing input and signature over it. */
	private static final String RFC8037_SIGNING_INPUT = "eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc";

	private static final String RFC8037_SIGNATURE = "hgyY0il_MGCjP0JzlnLWG1PPOt7-09PGcvMg3AIbQR6dWbhijcNR4ki4iylGjg5BhVsPt9g7sVvpAr_MuM0KAg";

	private static final String RFC8037_X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";

	@BeforeAll
	static void installProvider() {
		Ed25519TestKeys.installProvider();
	}

	/**
	 * RFC 8032 §7.1 TEST 1, TEST 2, TEST 3: secret key (seed), public key, message, signature.
	 */
	static Stream<Arguments> rfc8032Vectors() {
		return Stream.of(
				Arguments.of("TEST 1",
						"9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
						"d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
						"",
						"e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"),
				Arguments.of("TEST 2",
						"4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
						"3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
						"72",
						"92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00"),
				Arguments.of("TEST 3",
						"c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7",
						"fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025",
						"af82",
						"6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rfc8032Vectors")
	void testVerify_rfc8032Vector_true(String name, String seedHex, String publicHex, String messageHex,
			String signatureHex) {
		PublicKey key = Ed25519TestKeys.publicKeyFromRaw(Ed25519TestKeys.hex(publicHex));
		byte[] message = Ed25519TestKeys.hex(messageHex);
		byte[] signature = Ed25519TestKeys.hex(signatureHex);

		assertTrue(VERIFIER.verify(key, message, signature));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rfc8032Vectors")
	void testVerify_rfc8032Vector_seedDerivesPublicKeyAndSignature(String name, String seedHex, String publicHex,
			String messageHex, String signatureHex) {
		PrivateKey privateKey = Ed25519TestKeys.privateKeyFromSeed(Ed25519TestKeys.hex(seedHex));
		byte[] message = Ed25519TestKeys.hex(messageHex);

		// Ed25519 signing is deterministic, so the provider must reproduce the RFC signature exactly.
		assertArrayEquals(Ed25519TestKeys.hex(signatureHex), Ed25519TestKeys.sign(privateKey, message));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rfc8032Vectors")
	void testVerify_rfc8032VectorFlippedBit_false(String name, String seedHex, String publicHex, String messageHex,
			String signatureHex) {
		PublicKey key = Ed25519TestKeys.publicKeyFromRaw(Ed25519TestKeys.hex(publicHex));
		byte[] message = Ed25519TestKeys.hex(messageHex);
		byte[] signature = Ed25519TestKeys.hex(signatureHex);

		for (int index : new int[] { 0, 31, 32, 63 }) {
			byte[] flipped = signature.clone();
			flipped[index] ^= 0x01;
			assertFalse(VERIFIER.verify(key, message, flipped), "bit flipped at byte " + index);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rfc8032Vectors")
	void testVerify_rfc8032VectorDifferentMessage_false(String name, String seedHex, String publicHex,
			String messageHex, String signatureHex) {
		PublicKey key = Ed25519TestKeys.publicKeyFromRaw(Ed25519TestKeys.hex(publicHex));
		byte[] message = Ed25519TestKeys.hex(messageHex);
		byte[] signature = Ed25519TestKeys.hex(signatureHex);

		byte[] appended = Arrays.copyOf(message, message.length + 1);
		assertFalse(VERIFIER.verify(key, appended, signature));
		if (message.length > 0) {
			byte[] altered = message.clone();
			altered[0] ^= (byte) 0x80;
			assertFalse(VERIFIER.verify(key, altered, signature));
		}
	}

	@Test
	void testVerify_rfc8037AppendixA4_true() {
		PublicKey key = Ed25519PublicKeys.fromJwk(rfc8037Jwk());
		byte[] signingInput = RFC8037_SIGNING_INPUT.getBytes(StandardCharsets.US_ASCII);
		byte[] signature = StrictBase64Url.decode(RFC8037_SIGNATURE);

		assertTrue(VERIFIER.verify(key, signingInput, signature));
	}

	@Test
	void testVerify_rfc8037AppendixA4WrongKey_false() {
		PublicKey otherKey = Ed25519TestKeys.generateKeyPair().getPublic();
		byte[] signingInput = RFC8037_SIGNING_INPUT.getBytes(StandardCharsets.US_ASCII);
		byte[] signature = StrictBase64Url.decode(RFC8037_SIGNATURE);

		assertFalse(VERIFIER.verify(otherKey, signingInput, signature));
	}

	@Test
	void testVerify_generatedKeyPair_signThenVerify() {
		KeyPair pair = Ed25519TestKeys.generateKeyPair();
		byte[] message = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
		byte[] signature = Ed25519TestKeys.sign(pair.getPrivate(), message);

		assertTrue(VERIFIER.verify(pair.getPublic(), message, signature));
		assertTrue(VERIFIER.verify(Ed25519PublicKeys.fromJwk(Ed25519TestKeys.toJwk(pair.getPublic())), message,
				signature));
	}

	@Test
	void testVerify_signatureNot64Bytes_falseWithoutThrowing() {
		PublicKey key = Ed25519PublicKeys.fromJwk(rfc8037Jwk());
		byte[] signingInput = RFC8037_SIGNING_INPUT.getBytes(StandardCharsets.US_ASCII);
		byte[] signature = StrictBase64Url.decode(RFC8037_SIGNATURE);

		assertFalse(VERIFIER.verify(key, signingInput, Arrays.copyOf(signature, 63)));
		assertFalse(VERIFIER.verify(key, signingInput, Arrays.copyOf(signature, 65)));
		assertFalse(VERIFIER.verify(key, signingInput, new byte[0]));
		assertFalse(VERIFIER.verify(key, signingInput, null));
	}

	@Test
	void testVerify_allZeroAndAllFfSignatures_falseWithoutThrowing() {
		PublicKey key = Ed25519PublicKeys.fromJwk(rfc8037Jwk());
		byte[] signingInput = RFC8037_SIGNING_INPUT.getBytes(StandardCharsets.US_ASCII);

		byte[] zeros = new byte[64];
		byte[] ones = new byte[64];
		Arrays.fill(ones, (byte) 0xff);
		assertFalse(VERIFIER.verify(key, signingInput, zeros));
		// S >= L (group order): a malformed signature the provider must reject, never accept or throw.
		assertFalse(VERIFIER.verify(key, signingInput, ones));
	}

	@Test
	void testVerify_nullKeyOrMessage_throwsIllegalArgument() {
		PublicKey key = Ed25519PublicKeys.fromJwk(rfc8037Jwk());

		assertThrows(IllegalArgumentException.class, () -> VERIFIER.verify(null, new byte[0], new byte[64]));
		assertThrows(IllegalArgumentException.class, () -> VERIFIER.verify(key, null, new byte[64]));
	}

	private static Map<String, String> rfc8037Jwk() {
		Map<String, String> jwk = new HashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", RFC8037_X);
		return Collections.unmodifiableMap(jwk);
	}

}
