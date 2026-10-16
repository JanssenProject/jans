/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.lock.trace.crypto.TraceCryptoException;

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

	// -- thumbprint --------------------------------------------------------------------

	/** RFC 8037 Appendix A.3: SHA-256 of the canonical thumbprint input, in hex (verified against the RFC text). */
	private static final String RFC8037_THUMBPRINT_HEX = "90facafea9b1556698540f70c0117a22ea37bd5cf3ed3c47093c1707282b4b89";

	@Test
	void testThumbprint_rfc8037KnownAnswer_matches() {
		assertEquals("sha256:" + RFC8037_THUMBPRINT_HEX, Ed25519PublicKeys.thumbprint(validJwk()));
	}

	@Test
	void testThumbprint_extraJwkMembers_doNotChangeResult() {
		Map<String, String> jwk = validJwk();
		jwk.put("kid", "k1");
		jwk.put("use", "sig");
		jwk.put("d", RFC8037_D);

		assertEquals("sha256:" + RFC8037_THUMBPRINT_HEX, Ed25519PublicKeys.thumbprint(jwk));
	}

	@Test
	void testThumbprint_differentKeys_differ() {
		Map<String, String> other = Ed25519TestKeys.toJwk(Ed25519TestKeys.generateKeyPair().getPublic());

		assertNotEquals(Ed25519PublicKeys.thumbprint(validJwk()), Ed25519PublicKeys.thumbprint(other));
		assertEquals(Ed25519PublicKeys.thumbprint(other), Ed25519PublicKeys.thumbprint(other));
	}

	@Test
	void testThumbprint_missingOrMalformedX_throwsTraceCryptoException() {
		Map<String, String> noX = validJwk();
		noX.remove("x");
		assertEquals(TraceCryptoException.REASON_JWK_FORMAT,
				assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.thumbprint(noX)).getReason());
		assertEquals(TraceCryptoException.REASON_JWK_FORMAT,
				assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.thumbprint(null)).getReason());

		for (String badX : new String[] { "", "short", RFC8037_X + "=", RFC8037_X + "\"}", "a\"b" + RFC8037_X }) {
			Map<String, String> jwk = validJwk();
			jwk.put("x", badX);
			assertEquals(TraceCryptoException.REASON_JWK_X_ENCODING,
					assertThrows(TraceCryptoException.class, () -> Ed25519PublicKeys.thumbprint(jwk)).getReason(),
					badX);
		}
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
