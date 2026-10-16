/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.testkit.SignedAssertionFactory;
import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceHashes;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import io.jans.lock.trace.crypto.TraceVerificationKey;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;
import io.jans.trace.Ed25519Signer;
import io.jans.trace.JsonBody;
import io.jans.trace.Limits;
import io.jans.trace.SignedAssertion;
import io.jans.trace.UnsignedAssertion;

/**
 * The gate: both providers must produce byte-identical canonical text, content digests, key
 * thumbprints, receipt hashes and storage keys.
 */
class CryptoProviderParityTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final List<String> JCS_VECTORS = Arrays.asList("rfc-3.2.2-primitives", "rfc-3.2.3-sorting",
			"arrays", "french", "structures", "unicode", "weird");

	private static TraceCryptoProvider javaCrypto;

	private static NativeTraceCryptoProvider nativeProvider;

	@BeforeAll
	static void providers() {
		javaCrypto = TestProviders.javaProvider();
		nativeProvider = TestProviders.nativeProvider();
		assertTrue(javaCrypto.isAvailable());
		assertTrue(nativeProvider.isAvailable(), nativeProvider.describe());
	}

	// -- a. JCS ---------------------------------------------------------------------------------

	@Test
	void jcsVectors_everyFileCovered() {
		List<String> inputs = new ArrayList<>();
		for (String name : TestProviders.list(TraceTestKeys.class, "trace/jcs")) {
			if (name.endsWith(".input.json")) {
				inputs.add(name.substring(0, name.length() - ".input.json".length()));
			}
		}
		assertEquals(JCS_VECTORS.stream().sorted().collect(java.util.stream.Collectors.toList()), inputs);
	}

	/**
	 * The core refuses every number outside +-(2^53-1) by profile (jans-trace-core README, "strict
	 * JSON"), the Java canonicalizer writes it as an IEEE-754 double. {@code rfc-3.2.2-primitives}
	 * carries {@code 1E30}; it is the one vector the native provider must reject with
	 * {@code canonicalization_failed}.
	 */
	private static final List<String> NATIVE_REJECTED_VECTORS = Arrays.asList("rfc-3.2.2-primitives");

	@Test
	void canonicalize_jcsVectors_equalUnderBothAndEqualToExpected() throws Exception {
		List<String> rejected = new ArrayList<>();
		for (String vector : JCS_VECTORS) {
			JsonNode input = MAPPER.readTree(TestProviders.read(TraceTestKeys.class, "trace/jcs/" + vector + ".input.json"));
			String expected = TestProviders.read(TraceTestKeys.class, "trace/jcs/" + vector + ".output.json")
					.replaceAll("\\R$", "");

			String javaText = javaCrypto.canonicalize(input);
			assertEquals(expected, javaText, vector);
			try {
				assertEquals(expected, nativeProvider.canonicalize(input), vector);
			} catch (TraceCryptoException ex) {
				assertEquals(TraceCryptoException.REASON_CANONICALIZATION_FAILED, ex.getReason(), vector);
				rejected.add(vector);
			}
		}
		assertEquals(NATIVE_REJECTED_VECTORS, rejected);
	}

	@Test
	void canonicalize_primitivesWithoutOutOfRangeNumber_equalUnderBothAndEqualToExpected() throws Exception {
		String input = TestProviders.read(TraceTestKeys.class, "trace/jcs/rfc-3.2.2-primitives.input.json");
		String expected = TestProviders.read(TraceTestKeys.class, "trace/jcs/rfc-3.2.2-primitives.output.json")
				.replaceAll("\\R$", "");
		assertTrue(input.contains("1E30, "));
		assertTrue(expected.contains("1e+30,"));
		JsonNode node = MAPPER.readTree(input.replace("1E30, ", ""));

		String javaText = javaCrypto.canonicalize(node);

		assertEquals(expected.replace("1e+30,", ""), javaText);
		assertEquals(javaText, nativeProvider.canonicalize(node));
	}

	// -- b. verify + digest + thumbprint over every assertion fixture ----------------------------------

	/** Fixtures the core's assertion parser rejects although the Java provider verifies them (expected: none). */
	private static final List<String> NATIVE_REJECTED_FIXTURES = new ArrayList<>();

	@Test
	void verifyAndDigest_everyAssertionFixture_equalDigestUnderBoth() throws Exception {
		KeyPair keyPair = TraceTestKeys.generateKeyPair();
		Map<String, String> jwk = TraceTestKeys.toJwk(keyPair.getPublic());
		TraceVerificationKey javaKey = javaCrypto.parseKey(jwk);
		TraceVerificationKey nativeKey = nativeProvider.parseKey(jwk);
		assertEquals(javaKey.thumbprint(), nativeKey.thumbprint());

		List<String> fixtures = TestProviders.list(TestProviders.serviceAnchor(), "trace/assertions");
		assertFalse(fixtures.isEmpty());

		List<String> rejected = new ArrayList<>();
		int verified = 0;
		for (String fixture : fixtures) {
			String signedText = SignedAssertionFactory.fromFixture(fixture).sign(keyPair.getPrivate());
			ObjectNode root = (ObjectNode) MAPPER.readTree(signedText);
			byte[] signature = StrictBase64Url.decode(root.get("signature").asText());
			TraceSignedRecord record = new TraceSignedRecord(signedText, root, root.get("producer").asText(),
					root.get("kid").asText());

			String javaDigest = javaCrypto.verifyAndDigest(record, signature, javaKey);
			try {
				String nativeDigest = nativeProvider.verifyAndDigest(record, signature, nativeKey);
				assertEquals(javaDigest, nativeDigest, fixture);
				verified++;
			} catch (TraceCryptoException ex) {
				rejected.add(fixture + " -> " + ex.getReason() + " (" + ex.getMessage() + ")");
			}
		}
		System.out.println("parity (b): fixtures=" + fixtures.size() + " verifiedEqual=" + verified
				+ " nativeRejected=" + rejected);
		assertEquals(NATIVE_REJECTED_FIXTURES, rejected);
	}

	@Test
	void parseKey_manyKeys_equalThumbprint() {
		for (int i = 0; i < 20; i++) {
			Map<String, String> jwk = TraceTestKeys.toJwk(TraceTestKeys.generateKeyPair().getPublic());
			assertEquals(javaCrypto.parseKey(jwk).thumbprint(), nativeProvider.parseKey(jwk).thumbprint());
		}
	}

	// -- c. receipt hash ----------------------------------------------------------------------------

	@Test
	void receiptHash_grid_equalUnderBoth() {
		String digest = "sha256:" + "ab".repeat(32);
		String prev = "sha256:" + "1f".repeat(32);
		long[] times = { 0L, 1_800_000_000_000L, 1_800_000_000_123L, 1_800_000_000_007L };
		Object[][] positions = { { 1L, TraceHashes.ZERO_HASH }, { 2L, prev }, { 123_456_789_012L, prev } };

		int cells = 0;
		for (Object[] position : positions) {
			for (long receivedAt : times) {
				String javaHash = javaCrypto.receiptHash("domain-1", (Long) position[0], receivedAt, "producer-1",
						"record-1", digest, (String) position[1]);
				String nativeHash = nativeProvider.receiptHash("domain-1", (Long) position[0], receivedAt,
						"producer-1", "record-1", digest, (String) position[1]);
				assertEquals(javaHash, nativeHash, "seq=" + position[0] + " receivedAt=" + receivedAt);
				cells++;
			}
		}
		assertEquals(12, cells);
	}

	@Test
	void receiptHash_nonAsciiAndControlComponents_equalUnderBoth() {
		String digest = "sha256:" + "cd".repeat(32);
		String domain = "dömäin-€-😂";
		String producer = "prod\u0001ucer\"\\";
		String record = "rec\nord\u007f\u0080";

		assertEquals(javaCrypto.receiptHash(domain, 1L, 1_800_000_000_000L, producer, record, digest, TraceHashes.ZERO_HASH),
				nativeProvider.receiptHash(domain, 1L, 1_800_000_000_000L, producer, record, digest,
						TraceHashes.ZERO_HASH));
	}

	// -- d. storage keys ----------------------------------------------------------------------------

	private static final String[] AWKWARD = { "plain", "", "dömäin", "€ uro", "😂", "a\u0000b",
			"tab\there", "line\nbreak", "quote\"back\\slash", "\u007f\u0080\u009f", "</script>", "  ",
			"דּ", "coḿbined" };

	@Test
	void storageKeyInput_awkwardComponents_sameCanonicalTextAndHash() {
		for (String type : new String[] { "rec", "exec", "chain", "pos", "tok", "pkey", "rcpt" }) {
			for (String component : AWKWARD) {
				ArrayNode array = JsonNodeFactory.instance.arrayNode();
				array.add(type).add(component).add(component + component).add("3");

				String javaText = javaCrypto.canonicalize(array);
				String nativeText = nativeProvider.canonicalize(array);

				assertEquals(javaText, nativeText, type + " " + component);
				assertEquals(TraceHashes.sha256Hex(javaText.getBytes(StandardCharsets.UTF_8)),
						TraceHashes.sha256Hex(nativeText.getBytes(StandardCharsets.UTF_8)));
			}
		}
	}

	@Test
	void traceKeys_everyKeyFunction_equalUnderBoth() {
		TraceKeys javaKeys = new TraceKeys(javaCrypto);
		TraceKeys nativeKeys = new TraceKeys(nativeProvider);

		for (String component : AWKWARD) {
			RecordIdentity record = new RecordIdentity("d-" + component, "p-" + component, "r-" + component);
			ExecutionIdentity execution = new ExecutionIdentity("d-" + component, "a-" + component, "e-" + component);
			ChainIdentity chain = new ChainIdentity("d-" + component, "p-" + component, "i-" + component,
					"c-" + component);
			ChainPosition position = new ChainPosition(chain, 7L);
			TokenRef jti = new TokenRef("iss-" + component, "access_token", "jti-" + component, null);
			TokenRef fingerprint = new TokenRef("iss-" + component, "access_token", null,
					"sha256:" + "ef".repeat(32));

			assertEquals(javaKeys.recordKey(record), nativeKeys.recordKey(record), component);
			assertEquals(javaKeys.executionKey(execution), nativeKeys.executionKey(execution), component);
			assertEquals(javaKeys.chainKey(chain), nativeKeys.chainKey(chain), component);
			assertEquals(javaKeys.chainPositionKey(position), nativeKeys.chainPositionKey(position), component);
			assertEquals(javaKeys.tokenKey("d-" + component, jti), nativeKeys.tokenKey("d-" + component, jti),
					component);
			assertEquals(javaKeys.tokenKey("d-" + component, fingerprint),
					nativeKeys.tokenKey("d-" + component, fingerprint), component);
			assertEquals(javaKeys.producerKeyKey("d-" + component, "p-" + component, "k-" + component),
					nativeKeys.producerKeyKey("d-" + component, "p-" + component, "k-" + component), component);
			assertEquals(javaKeys.receiptKey("d-" + component, 42L), nativeKeys.receiptKey("d-" + component, 42L),
					component);
		}
	}

	// -- e. cross-signing: native signs, Java verifies -----------------------------------------------

	@Test
	void crossSigning_nativeSignedRecord_verifiesUnderJava() throws Exception {
		byte[] secret = TraceTestKeys.hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
		for (String fixture : Arrays.asList(SignedAssertionFactory.FIXTURE_AUTHORIZATION_DECISION,
				SignedAssertionFactory.FIXTURE_CAPABILITY_INVOKED, SignedAssertionFactory.FIXTURE_RUNTIME_EFFECT)) {
			ObjectNode root = (ObjectNode) MAPPER.readTree(TestProviders.read(TestProviders.serviceAnchor(),
					"trace/assertions/" + fixture));
			root.remove("signature");
			String kid = root.get("kid").asText();
			String producer = root.get("producer").asText();

			Ed25519Signer signer = Ed25519Signer.fromSecretBytes(kid, secret);
			SignedAssertion signed = UnsignedAssertion.parse(new JsonBody(root.toString()), new Limits()).sign(signer);

			Map<String, String> jwk = new TreeMap<>();
			jwk.put("kty", "OKP");
			jwk.put("crv", "Ed25519");
			jwk.put("x", StrictBase64Url.encode(signer.publicKeyBytes()));
			TraceVerificationKey javaKey = javaCrypto.parseKey(jwk);
			TraceVerificationKey nativeKey = nativeProvider.parseKey(jwk);
			assertEquals(javaKey.thumbprint(), nativeKey.thumbprint());

			ObjectNode signedRoot = (ObjectNode) MAPPER.readTree(signed.text());
			byte[] signature = StrictBase64Url.decode(signedRoot.get("signature").asText());
			TraceSignedRecord record = new TraceSignedRecord(signed.text(), signedRoot, producer, kid);

			String javaDigest = javaCrypto.verifyAndDigest(record, signature, javaKey);

			assertNotNull(javaDigest);
			assertEquals(signed.contentDigest().toString(), javaDigest, fixture);
			assertEquals(javaDigest, nativeProvider.verifyAndDigest(record, signature, nativeKey), fixture);
		}
	}

	@Test
	void verifyAndDigest_tamperedRecord_bothReportSignatureMismatch() throws Exception {
		KeyPair keyPair = TraceTestKeys.generateKeyPair();
		Map<String, String> jwk = TraceTestKeys.toJwk(keyPair.getPublic());
		String signedText = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());
		ObjectNode root = (ObjectNode) MAPPER.readTree(signedText);
		root.put("record_id", "tampered-record-id");
		byte[] signature = StrictBase64Url.decode(root.get("signature").asText());
		TraceSignedRecord record = new TraceSignedRecord(root.toString(), root, root.get("producer").asText(),
				root.get("kid").asText());

		for (TraceCryptoProvider provider : Arrays.asList(javaCrypto, nativeProvider)) {
			TraceCryptoException ex = assertThrows(TraceCryptoException.class,
					() -> provider.verifyAndDigest(record, signature, provider.parseKey(jwk)));
			assertEquals(TraceCryptoException.REASON_SIGNATURE_MISMATCH, ex.getReason());
		}
	}

}
