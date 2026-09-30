/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.service.trace.error.TraceCryptoException;

class Ed25519PublicKeysTest {

	/** RFC 8037 Appendix A.1 public key (same key as RFC 8032 §7.1 TEST 1). */
	private static final String RFC8037_X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";

	private static final String RFC8037_X_HEX = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a";

	/** RFC 8037 Appendix A.1 private key member. */
	private static final String RFC8037_D = "nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A";

	@BeforeAll
	static void installProvider() {
		Ed25519TestKeys.installProvider();
	}

	@Test
	void testFromJwk_map_rfc8037Key_buildsSpkiEncodedKey() {
		PublicKey key = Ed25519PublicKeys.fromJwk(validJwk());

		assertNotNull(key);
		assertArrayEquals(Ed25519TestKeys.hex("302a300506032b6570032100" + RFC8037_X_HEX), key.getEncoded());
		assertArrayEquals(Ed25519TestKeys.hex(RFC8037_X_HEX), Ed25519TestKeys.rawPublicKey(key));
	}

	@Test
	void testFromJwk_jsonNode_rfc8037Key_buildsSameKeyAsMap() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("kty", "OKP");
		node.put("crv", "Ed25519");
		node.put("x", RFC8037_X);
		node.put("kid", "ignored");
		node.put("use", "sig");

		PublicKey fromNode = Ed25519PublicKeys.fromJwk(node);

		assertArrayEquals(Ed25519PublicKeys.fromJwk(validJwk()).getEncoded(), fromNode.getEncoded());
	}

	@Test
	void testFromJwk_map_ignoresUnknownMembers() {
		Map<String, String> jwk = validJwk();
		jwk.put("kid", "producer-2026-01");
		jwk.put("alg", "EdDSA");

		assertNotNull(Ed25519PublicKeys.fromJwk(jwk));
	}

	@Test
	void testFromJwk_wrongKty_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("kty", "EC");

		assertReason(TraceCryptoException.REASON_JWK_KTY, jwk);
	}

	@Test
	void testFromJwk_missingKty_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.remove("kty");

		assertReason(TraceCryptoException.REASON_JWK_KTY, jwk);
	}

	@Test
	void testFromJwk_lowercaseKty_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("kty", "okp");

		assertReason(TraceCryptoException.REASON_JWK_KTY, jwk);
	}

	@Test
	void testFromJwk_wrongCrv_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("crv", "Ed448");

		assertReason(TraceCryptoException.REASON_JWK_CRV, jwk);
	}

	@Test
	void testFromJwk_missingCrv_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.remove("crv");

		assertReason(TraceCryptoException.REASON_JWK_CRV, jwk);
	}

	@Test
	void testFromJwk_presentD_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("d", RFC8037_D);

		assertReason(TraceCryptoException.REASON_JWK_PRIVATE_MATERIAL, jwk);
	}

	@Test
	void testFromJwk_presentNullD_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("d", null);

		assertReason(TraceCryptoException.REASON_JWK_PRIVATE_MATERIAL, jwk);
	}

	@Test
	void testFromJwk_jsonNode_presentD_rejected() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("kty", "OKP");
		node.put("crv", "Ed25519");
		node.put("x", RFC8037_X);
		node.put("d", RFC8037_D);

		TraceCryptoException ex = assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.fromJwk(node));
		assertEquals(TraceCryptoException.REASON_JWK_PRIVATE_MATERIAL, ex.getReason());
	}

	@Test
	void testFromJwk_x31Bytes_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("x", StrictBase64Url.encode(new byte[31]));

		assertReason(TraceCryptoException.REASON_JWK_X_LENGTH, jwk);
	}

	@Test
	void testFromJwk_x33Bytes_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("x", StrictBase64Url.encode(new byte[33]));

		assertReason(TraceCryptoException.REASON_JWK_X_LENGTH, jwk);
	}

	@Test
	void testFromJwk_x44ByteSpkiBlob_rejected() {
		// The jans-auth crypto provider convention (full SPKI in x) is not RFC 8037 and must fail.
		Map<String, String> jwk = validJwk();
		jwk.put("x", StrictBase64Url.encode(Ed25519TestKeys.hex("302a300506032b6570032100" + RFC8037_X_HEX)));

		assertReason(TraceCryptoException.REASON_JWK_X_LENGTH, jwk);
	}

	@Test
	void testFromJwk_paddedX_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("x", RFC8037_X + "=");

		assertReason(TraceCryptoException.REASON_JWK_X_ENCODING, jwk);
	}

	@Test
	void testFromJwk_standardBase64X_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.put("x", RFC8037_X.replace('_', '/'));

		assertReason(TraceCryptoException.REASON_JWK_X_ENCODING, jwk);
	}

	@Test
	void testFromJwk_missingX_rejected() {
		Map<String, String> jwk = validJwk();
		jwk.remove("x");

		assertReason(TraceCryptoException.REASON_JWK_X_ENCODING, jwk);
	}

	@Test
	void testFromJwk_jsonNode_numericX_rejected() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("kty", "OKP");
		node.put("crv", "Ed25519");
		node.put("x", 42);

		TraceCryptoException ex = assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.fromJwk(node));
		assertEquals(TraceCryptoException.REASON_JWK_X_ENCODING, ex.getReason());
	}

	@Test
	void testFromJwk_nullMap_rejected() {
		TraceCryptoException ex = assertThrows(TraceCryptoException.class,
				() -> Ed25519PublicKeys.fromJwk((Map<String, String>) null));
		assertEquals(TraceCryptoException.REASON_JWK_FORMAT, ex.getReason());
	}

	@Test
	void testFromJwk_nonObjectNode_rejected() {
		JsonNode array = JsonNodeFactory.instance.arrayNode();

		TraceCryptoException ex = assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.fromJwk(array));
		assertEquals(TraceCryptoException.REASON_JWK_FORMAT, ex.getReason());
	}

	@Test
	void testFromRaw_wrongLength_rejected() {
		TraceCryptoException ex = assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.fromRaw(new byte[0]));
		assertEquals(TraceCryptoException.REASON_JWK_X_LENGTH, ex.getReason());
	}

	@Test
	void testFromJwk_generatedKeyRoundTrip_matchesOriginalEncoding() {
		PublicKey original = Ed25519TestKeys.generateKeyPair().getPublic();

		PublicKey rebuilt = Ed25519PublicKeys.fromJwk(Ed25519TestKeys.toJwk(original));

		assertArrayEquals(original.getEncoded(), rebuilt.getEncoded());
	}

	private static Map<String, String> validJwk() {
		Map<String, String> jwk = new HashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", RFC8037_X);
		return jwk;
	}

	private static void assertReason(String expectedReason, Map<String, String> jwk) {
		TraceCryptoException ex = assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.fromJwk(jwk));
		assertEquals(expectedReason, ex.getReason());
	}

}
