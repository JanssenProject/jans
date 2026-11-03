/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.bc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import io.jans.lock.trace.crypto.TraceHashes;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import io.jans.lock.trace.crypto.TraceVerificationKey;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;

class JavaTraceCryptoProviderTest {

	/** RFC 8037 Appendix A.2 public key. */
	private static final String RFC8037_X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";

	private static final String RFC8037_THUMBPRINT = "sha256:90facafea9b1556698540f70c0117a22ea37bd5cf3ed3c47093c1707282b4b89";

	private JavaTraceCryptoProvider provider;

	@BeforeAll
	static void installProvider() {
		TraceTestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		Ed25519Capability capability = new Ed25519Capability();
		capability.probe();
		provider = new JavaTraceCryptoProvider(capability, new Ed25519Verifier());
	}

	@Test
	void testType_isJava() {
		assertEquals(TraceCryptoProviderType.JAVA, provider.type());
	}

	@Test
	void testAvailability_afterProbe_available() {
		assertTrue(provider.isAvailable());
		provider.requireAvailable();
		assertTrue(provider.describe().startsWith("java/"));
	}

	@Test
	void testAvailability_withoutProbe_failsClosed() {
		Ed25519Capability unprobed = new Ed25519Capability();
		JavaTraceCryptoProvider unavailable = new JavaTraceCryptoProvider(unprobed, new Ed25519Verifier());

		// A new capability is unavailable until probe() runs; @PostConstruct does not run outside CDI.
		assertFalse(unavailable.isAvailable());
		TraceCryptoException ex = assertThrows(TraceCryptoException.class, unavailable::requireAvailable);
		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, ex.getReason());
	}

	@Test
	void testCanonicalize_sortsMembers() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("b", 2);
		node.put("a", "x");

		assertEquals("{\"a\":\"x\",\"b\":2}", provider.canonicalize(node));
	}

	@Test
	void testCanonicalize_pojoNode_canonicalizationFailed() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.putPOJO("a", new Object());

		TraceCryptoException ex = assertThrows(TraceCryptoException.class, () -> provider.canonicalize(node));
		assertEquals(TraceCryptoException.REASON_CANONICALIZATION_FAILED, ex.getReason());
	}

	@Test
	void testParseKey_rfc8037Vector_thumbprintMatches() {
		Map<String, String> jwk = new HashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", RFC8037_X);

		assertEquals(RFC8037_THUMBPRINT, provider.parseKey(jwk).thumbprint());
	}

	@Test
	void testVerifyAndDigest_validSignature_returnsDigestOfCompleteRecord() {
		KeyPair pair = TraceTestKeys.generateKeyPair();
		TraceVerificationKey key = provider.parseKey(TraceTestKeys.toJwk(pair.getPublic()));
		ObjectNode root = signedRecord(pair);
		byte[] signature = StrictBase64Url.decode(root.get("signature").textValue());

		String digest = provider.verifyAndDigest(record(root), signature, key);

		assertEquals(CanonicalHashes.sha256PrefixedOfJcs(root), digest);
		assertTrue(digest.startsWith(TraceHashes.SHA256_PREFIX));
	}

	@Test
	void testVerifyAndDigest_doesNotModifyRecord() {
		KeyPair pair = TraceTestKeys.generateKeyPair();
		TraceVerificationKey key = provider.parseKey(TraceTestKeys.toJwk(pair.getPublic()));
		ObjectNode root = signedRecord(pair);
		ObjectNode before = root.deepCopy();
		byte[] signature = StrictBase64Url.decode(root.get("signature").textValue());

		provider.verifyAndDigest(record(root), signature, key);

		assertEquals(before, root);
	}

	@Test
	void testVerifyAndDigest_flippedSignatureByte_signatureMismatch() {
		KeyPair pair = TraceTestKeys.generateKeyPair();
		TraceVerificationKey key = provider.parseKey(TraceTestKeys.toJwk(pair.getPublic()));
		ObjectNode root = signedRecord(pair);
		byte[] signature = StrictBase64Url.decode(root.get("signature").textValue());
		signature[10] ^= 0x01;

		TraceCryptoException ex = assertThrows(TraceCryptoException.class,
				() -> provider.verifyAndDigest(record(root), signature, key));
		assertEquals(TraceCryptoException.REASON_SIGNATURE_MISMATCH, ex.getReason());
	}

	@Test
	void testVerifyAndDigest_pojoNode_canonicalizationFailed() {
		KeyPair pair = TraceTestKeys.generateKeyPair();
		TraceVerificationKey key = provider.parseKey(TraceTestKeys.toJwk(pair.getPublic()));
		ObjectNode root = JsonNodeFactory.instance.objectNode();
		root.putPOJO("a", new Object());
		root.put("signature", "x");

		TraceCryptoException ex = assertThrows(TraceCryptoException.class,
				() -> provider.verifyAndDigest(record(root), new byte[64], key));
		assertEquals(TraceCryptoException.REASON_CANONICALIZATION_FAILED, ex.getReason());
	}

	@Test
	void testVerifyAndDigest_foreignKey_illegalArgument() {
		KeyPair pair = TraceTestKeys.generateKeyPair();
		ObjectNode root = signedRecord(pair);
		TraceVerificationKey foreign = () -> "sha256:" + "0".repeat(64);

		assertThrows(IllegalArgumentException.class,
				() -> provider.verifyAndDigest(record(root), new byte[64], foreign));
	}

	@Test
	void testReceiptHash_equalsReceiptHashes() {
		String contentDigest = "sha256:" + "a".repeat(64);

		String actual = provider.receiptHash("domain-1", 1L, 1_800_000_000_000L, "producer-1", "record-1",
				contentDigest, TraceHashes.ZERO_HASH);

		assertEquals("sha256:cd66cd1b3c53dbdbdebc76e313a3ba72cbf3460d9c05cddf8e09216eb03e95b0", actual);
		assertEquals(ReceiptHashes.receiptHash("domain-1", 1L, 1_800_000_000_000L, "producer-1", "record-1",
				contentDigest, TraceHashes.ZERO_HASH), actual);
	}

	@Test
	void testParseKey_sameKeyInstanceUsable_acrossCalls() {
		KeyPair pair = TraceTestKeys.generateKeyPair();
		TraceVerificationKey key = provider.parseKey(TraceTestKeys.toJwk(pair.getPublic()));
		ObjectNode root = signedRecord(pair);
		byte[] signature = StrictBase64Url.decode(root.get("signature").textValue());

		String first = provider.verifyAndDigest(record(root), signature, key);
		String second = provider.verifyAndDigest(record(root), signature, key);

		assertEquals(first, second);
	}

	private static ObjectNode signedRecord(KeyPair pair) {
		ObjectNode root = JsonNodeFactory.instance.objectNode();
		root.put("record_id", "r-1");
		root.put("n", 1);
		byte[] message = JcsCanonicalizer.canonicalizeToUtf8(root);
		byte[] signature = TraceTestKeys.sign(pair.getPrivate(), message);
		root.put("signature", StrictBase64Url.encode(signature));
		return root;
	}

	private static TraceSignedRecord record(ObjectNode root) {
		return new TraceSignedRecord(root.toString(), root, "producer-1", "kid-1");
	}

}
