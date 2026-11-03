/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import io.jans.lock.trace.crypto.TraceHashes;
import io.jans.lock.trace.crypto.TraceVerificationKey;
import io.jans.trace.Limits;

class NativeTraceCryptoProviderTest {

	/** RFC 8037 Appendix A.1 public key. */
	private static final String RFC8037_X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";

	private static final String RFC8037_D = "nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A";

	/** RFC 8037 Appendix A.3 thumbprint, as in {@code Ed25519PublicKeysTest}. */
	private static final String RFC8037_THUMBPRINT = "sha256:90facafea9b1556698540f70c0117a22ea37bd5cf3ed3c47093c1707282b4b89";

	private static final String DIGEST_A = "sha256:" + "a".repeat(64);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static NativeTraceCryptoProvider provider;

	@BeforeAll
	static void probe() {
		provider = new NativeTraceCryptoProvider(new TraceConfiguration());
		provider.probe();
	}

	@Test
	void probe_nativeLibraryLoads_available() {
		assertTrue(provider.isAvailable(), provider.describe());
		provider.requireAvailable();
		assertEquals(TraceCryptoProviderType.NATIVE, provider.type());
		assertTrue(provider.describe().startsWith("native/jans-trace-core "), provider.describe());
	}

	@Test
	void requireAvailable_notProbed_failsClosed() {
		NativeTraceCryptoProvider unprobed = new NativeTraceCryptoProvider(new TraceConfiguration());

		assertFalse(unprobed.isAvailable());
		assertReason(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, unprobed::requireAvailable);
		assertReason(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, () -> unprobed.parseKey(validJwk()));
		assertReason(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
				() -> unprobed.receiptHash("d", 1L, 0L, "p", "r", DIGEST_A, TraceHashes.ZERO_HASH));
		assertReason(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
				() -> unprobed.canonicalize(MAPPER.createObjectNode()));
	}

	@Test
	void probe_failure_neverThrowsAndFailsClosed() {
		NativeTraceCryptoProvider broken = new NativeTraceCryptoProvider();

		broken.probe();

		assertFalse(broken.isAvailable());
		assertReason(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, broken::requireAvailable);
		assertTrue(broken.describe().startsWith("NullPointerException"), broken.describe());
	}

	@Test
	void buildLimits_defaultConfiguration_coreDefaultsDominate() {
		Limits limits = NativeTraceCryptoProvider.buildLimits(new TraceConfiguration());

		assertEquals(524_288L, limits.maxInputBytes());
		assertEquals(64L, limits.maxDepth());
		assertEquals(1024L, limits.maxObjectMembers());
		assertEquals(1024L, limits.maxArrayElements());
		assertEquals(65_536L, limits.maxStringBytes());
		assertEquals(524_288L, limits.maxCanonicalBytes());
	}

	@Test
	void buildLimits_largeLockValues_scaleUp() {
		TraceConfiguration config = new TraceConfiguration();
		config.setMaxRequestBytes(1_000_000);
		config.setMaxJsonDepth(100);
		config.setMaxObjectMembers(4_000);
		config.setMaxArrayLength(5_000);
		config.setMaxStringLength(50_000);

		Limits limits = NativeTraceCryptoProvider.buildLimits(config);

		assertEquals(2_000_000L, limits.maxInputBytes());
		assertEquals(200L, limits.maxDepth());
		assertEquals(8_000L, limits.maxObjectMembers());
		assertEquals(10_000L, limits.maxArrayElements());
		assertEquals(200_000L, limits.maxStringBytes());
		assertEquals(2_000_000L, limits.maxCanonicalBytes());
	}

	@Test
	void onConfigurationUpdate_raisedLimits_limitsRefreshed() throws ReflectiveOperationException {
		NativeTraceCryptoProvider injected = new NativeTraceCryptoProvider();
		setField(injected, "appConfiguration", new AppConfiguration());
		injected.probe();
		assertTrue(injected.isAvailable(), injected.describe());
		assertEquals(524_288L, limitsOf(injected).maxInputBytes());

		AppConfiguration updated = new AppConfiguration();
		updated.getTraceConfiguration().setMaxRequestBytes(1_000_000);
		injected.onConfigurationUpdate(updated);

		assertEquals(2_000_000L, limitsOf(injected).maxInputBytes());
		assertTrue(injected.isAvailable());
		assertTrue(injected.describe().startsWith("native/jans-trace-core "), injected.describe());
	}

	@Test
	void onConfigurationUpdate_unavailableProvider_ignored() throws ReflectiveOperationException {
		NativeTraceCryptoProvider unprobed = new NativeTraceCryptoProvider();

		unprobed.onConfigurationUpdate(new AppConfiguration());

		assertFalse(unprobed.isAvailable());
		assertNull(limitsOf(unprobed));
	}

	private static Limits limitsOf(NativeTraceCryptoProvider target) throws ReflectiveOperationException {
		Field field = NativeTraceCryptoProvider.class.getDeclaredField("limits");
		field.setAccessible(true);
		return (Limits) field.get(target);
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	void parseKey_rfc8037Jwk_thumbprintMatchesVector() {
		TraceVerificationKey key = provider.parseKey(validJwk());

		assertEquals(RFC8037_THUMBPRINT, key.thumbprint());
	}

	@Test
	void parseKey_metadataMembersIgnored() {
		Map<String, String> jwk = validJwk();
		jwk.put("kid", "k1");
		jwk.put("use", "sig");

		assertEquals(RFC8037_THUMBPRINT, provider.parseKey(jwk).thumbprint());
	}

	@Test
	void parseKey_null_jwkFormat() {
		assertReason(TraceCryptoException.REASON_JWK_FORMAT, () -> provider.parseKey(null));
	}

	/**
	 * The core reports every JWK content problem (private {@code d}, {@code kty}, {@code crv},
	 * {@code x}) as {@code INVALID_PUBLIC_KEY}, which the provider maps to {@code invalid_key}; only
	 * JSON-level errors map to {@code jwk_format}. The Java provider names finer reasons
	 * ({@code jwk_kty}, ...); the HTTP error id ({@code invalid_key}) is the same.
	 */
	@Test
	void parseKey_privateMaterial_invalidKey() {
		Map<String, String> jwk = validJwk();
		jwk.put("d", RFC8037_D);

		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(jwk));
	}

	@Test
	void parseKey_wrongKtyOrCrv_invalidKey() {
		Map<String, String> wrongKty = validJwk();
		wrongKty.put("kty", "RSA");
		Map<String, String> wrongCrv = validJwk();
		wrongCrv.put("crv", "X25519");
		Map<String, String> noCrv = validJwk();
		noCrv.remove("crv");

		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(wrongKty));
		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(wrongCrv));
		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(noCrv));
	}

	@Test
	void parseKey_badX_invalidKey() {
		Map<String, String> padded = validJwk();
		padded.put("x", RFC8037_X + "=");
		Map<String, String> missing = validJwk();
		missing.remove("x");
		Map<String, String> shortX = validJwk();
		shortX.put("x", RFC8037_X.substring(0, 20));

		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(padded));
		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(missing));
		assertReason(TraceCryptoException.REASON_INVALID_KEY, () -> provider.parseKey(shortX));
	}

	@Test
	void receiptHash_knownAnswer_zeroMillis() {
		assertEquals("sha256:cd66cd1b3c53dbdbdebc76e313a3ba72cbf3460d9c05cddf8e09216eb03e95b0",
				provider.receiptHash("domain-1", 1L, 1_800_000_000_000L, "producer-1", "record-1", DIGEST_A,
						TraceHashes.ZERO_HASH));
	}

	@Test
	void receiptHash_knownAnswer_nonZeroMillis() {
		assertEquals("sha256:ef30fe233db4f3adf4b5df5f0755faf44b8b3fbf3e15b2ea4bd7be94cc6b8fff",
				provider.receiptHash("domain-1", 1L, 1_800_000_000_123L, "producer-1", "record-1", DIGEST_A,
						TraceHashes.ZERO_HASH));
	}

	@Test
	void receiptHash_sequenceTwoWithGenesisPrev_invalidReceiptInput() {
		assertReason(TraceCryptoException.REASON_INVALID_RECEIPT_INPUT, () -> provider.receiptHash("domain-1", 2L,
				1_800_000_000_000L, "producer-1", "record-1", DIGEST_A, TraceHashes.ZERO_HASH));
	}

	@Test
	void receiptHash_sequenceOneWithNonGenesisPrev_invalidReceiptInput() {
		assertReason(TraceCryptoException.REASON_INVALID_RECEIPT_INPUT, () -> provider.receiptHash("domain-1", 1L,
				1_800_000_000_000L, "producer-1", "record-1", DIGEST_A, DIGEST_A));
	}

	@Test
	void receiptHash_malformedDigest_invalidReceiptInput() {
		assertReason(TraceCryptoException.REASON_INVALID_RECEIPT_INPUT, () -> provider.receiptHash("domain-1", 1L,
				1_800_000_000_000L, "producer-1", "record-1", "sha256:xyz", TraceHashes.ZERO_HASH));
	}

	@Test
	void canonicalize_smallTree_rfc8785Text() throws Exception {
		JsonNode node = MAPPER.readTree("{\"b\": 1, \"a\": [true, null, \"x\"], \"\u00e9\": \"\\u000a\"}");

		assertEquals("{\"a\":[true,null,\"x\"],\"b\":1,\"\u00e9\":\"\\n\"}", provider.canonicalize(node));
	}

	/**
	 * The binding throws a raw {@code CharacterCodingException} for a string with an unpaired
	 * surrogate; the provider turns it into the operation's reason instead of letting it escape.
	 */
	@Test
	void unpairedSurrogate_mappedToOperationReason() throws Exception {
		JsonNode node = MAPPER.readTree("{\"v\":\"\\ud800\"}");
		Map<String, String> jwk = validJwk();
		jwk.put("x", "\ud800");

		assertReason(TraceCryptoException.REASON_CANONICALIZATION_FAILED, () -> provider.canonicalize(node));
		assertReason(TraceCryptoException.REASON_JWK_FORMAT, () -> provider.parseKey(jwk));
		assertReason(TraceCryptoException.REASON_INVALID_RECEIPT_INPUT, () -> provider.receiptHash("d\ud800", 1L,
				1_800_000_000_000L, "producer-1", "record-1", DIGEST_A, TraceHashes.ZERO_HASH));
	}

	private static Map<String, String> validJwk() {
		Map<String, String> jwk = new HashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", RFC8037_X);
		return jwk;
	}

	private static void assertReason(String reason, org.junit.jupiter.api.function.Executable call) {
		TraceCryptoException ex = assertThrows(TraceCryptoException.class, call);
		assertEquals(reason, ex.getReason(), ex.getMessage());
	}

}
