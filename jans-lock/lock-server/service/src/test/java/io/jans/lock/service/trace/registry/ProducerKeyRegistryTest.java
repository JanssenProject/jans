/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.store.InMemoryTraceStore;

/**
 * Tests for {@link ProducerKeyRegistry}: the key validity matrix, create-only
 * registration, idempotent revocation and the "stored key unusable" data-error path.
 */
class ProducerKeyRegistryTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String KID = "kid-1";

	private static final String REGISTERED_BY = "2200.abcd";

	private static final List<String> ALL_KINDS = List.of(TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION,
			TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, TraceConstants.EVENT_KIND_RUNTIME_EFFECT);

	private InMemoryTraceStore store;

	private ProducerKeyRegistry registry;

	private Map<String, String> jwk;

	@BeforeAll
	static void installProvider() {
		TraceTestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		registry = new ProducerKeyRegistry();
		setField(registry, "traceStore", store);
		setField(registry, "log", LoggerFactory.getLogger(ProducerKeyRegistry.class));
		setField(registry, "cryptoProvider", TraceTestCrypto.javaProvider());

		KeyPair keyPair = TraceTestKeys.generateKeyPair();
		jwk = TraceTestKeys.toJwk(keyPair.getPublic());
	}

	// -- registration ---------------------------------------------------------------------------

	@Test
	void testRegister_NewKey_Succeeds() {
		TraceProducerKeyEntry key = registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);

		assertEquals(DOMAIN, key.getDomainId());
		assertEquals(PRODUCER, key.getProducerId());
		assertEquals(KID, key.getKid());
		assertEquals(1000L, key.getValidFromMs());
		assertEquals(null, key.getValidUntilMs());
		assertEquals(null, key.getRevokedAtMs());
		assertEquals(1000L, key.getCreatedAtMs());
		assertTrue(store.findProducerKey(DOMAIN, PRODUCER, KID).isPresent());
	}

	@Test
	void testRegister_SameKeyTwice_KeyAlreadyExists() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);

		TraceConflictException ex = assertThrows(TraceConflictException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.KEY_ALREADY_EXISTS, ex.getErrorId());
	}

	@Test
	void testRegister_ValidUntilBeforeValidFrom_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 2000L, 1000L, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_VALIDITY_WINDOW, ex.getReason());
	}

	@Test
	void testRegister_ValidUntilEqualsValidFrom_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, 1000L, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_VALIDITY_WINDOW, ex.getReason());
	}

	@Test
	void testRegister_ProducerIdBadFormat_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, "-bad producer", KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_PRODUCER_ID_FORMAT, ex.getReason());
	}

	@Test
	void testRegister_EmptyKid_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, "", jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_KID_EMPTY, ex.getReason());
	}

	@Test
	void testRegister_KidTooLong_InvalidKey() {
		String longKid = repeat('k', 256);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, longKid, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_KID_LENGTH, ex.getReason());
	}

	@Test
	void testRegister_MalformedJwk_PropagatesTraceCryptoException() {
		Map<String, String> badJwk = new LinkedHashMap<>();
		badJwk.put("kty", "RSA");

		assertThrows(TraceCryptoException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, badJwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L));
	}

	// -- revocation -------------------------------------------------------------------------------

	@Test
	void testRevoke_UnknownKey_ReturnsEmpty() {
		Optional<TraceProducerKeyEntry> result = registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		assertFalse(result.isPresent());
	}

	@Test
	void testRevoke_KnownKey_SetsRevokedAt() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);

		Optional<TraceProducerKeyEntry> result = registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		assertTrue(result.isPresent());
		assertEquals(Long.valueOf(5000L), result.get().getRevokedAtMs());
	}

	@Test
	void testRevoke_Twice_IdempotentSameRevokedAt() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		Optional<TraceProducerKeyEntry> result = registry.revoke(DOMAIN, PRODUCER, KID, 9000L);

		assertTrue(result.isPresent());
		assertEquals(Long.valueOf(5000L), result.get().getRevokedAtMs());
	}

	// -- resolveForVerification: key validity matrix ----------------------------

	@Test
	void testResolveForVerification_UnknownKey_UnknownProducerKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_KEY_NOT_REGISTERED, ex.getReason());
	}

	@Test
	void testResolveForVerification_BeforeValidFrom_NotYetValid() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 5000L, null, REGISTERED_BY, 1000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 4999L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_NOT_YET_VALID, ex.getReason());
	}

	@Test
	void testResolveForVerification_InsideWindow_Resolves() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, 9000L, REGISTERED_BY, 1000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);

		assertEquals(KID, resolved.getKey().getKid());
		assertTrue(resolved.getVerificationKey() != null);
		assertFalse(resolved.getVerificationKey().thumbprint().isEmpty());
	}

	@Test
	void testResolveForVerification_NoValidUntil_ResolvesFarInTheFuture() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID,
				Long.MAX_VALUE / 2);

		assertEquals(KID, resolved.getKey().getKid());
	}

	@Test
	void testResolveForVerification_AtValidFrom_Resolves() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, 9000L, REGISTERED_BY, 1000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 1000L);

		assertEquals(KID, resolved.getKey().getKid());
	}

	@Test
	void testResolveForVerification_AfterValidUntil_Expired() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, 9000L, REGISTERED_BY, 1000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 9000L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_EXPIRED, ex.getReason());
	}

	@Test
	void testResolveForVerification_RevokedInThePast_Revoked() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 6000L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_REVOKED, ex.getReason());
	}

	@Test
	void testResolveForVerification_RevokedInTheFuture_StillValidNow() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		registry.revoke(DOMAIN, PRODUCER, KID, 9000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);

		assertEquals(KID, resolved.getKey().getKid());
	}

	@Test
	void testResolveForVerification_StoredKeyUnusable_UnknownProducerKey() throws Exception {
		Map<String, String> badJwk = new LinkedHashMap<>();
		badJwk.put("kty", "OKP");
		badJwk.put("crv", "Ed25519");
		badJwk.put("x", "not-valid-base64url!!");
		TraceProducerKeyEntry key = new TraceProducerKeyEntry();
		key.setDomainId(DOMAIN);
		key.setProducerId(PRODUCER);
		key.setKid(KID);
		key.setPublicKeyJwk(badJwk);
		key.setValidFrom(new Date(1000L));
		key.setRegisteredBy(REGISTERED_BY);
		key.setCreationDate(new Date(1000L));
		store.insertProducerKey(key);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_STORED_KEY_UNUSABLE, ex.getReason());
	}

	// -- authorized event kinds -----------------------------------------------------------

	@Test
	void testRegister_AuthorizedEventKinds_StoredAndResolved() {
		List<String> kinds = List.of(TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION,
				TraceConstants.EVENT_KIND_RUNTIME_EFFECT);

		TraceProducerKeyEntry key = registry.register(DOMAIN, PRODUCER, KID, jwk, kinds, 1000L, null, REGISTERED_BY,
				1000L);

		assertEquals(kinds, key.getAuthorizedEventKinds());
		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);
		assertEquals(kinds, resolved.getAuthorizedEventKinds());
		assertEquals(kinds, store.findProducerKey(DOMAIN, PRODUCER, KID).get().getAuthorizedEventKinds());
	}

	@Test
	void testRegister_AuthorizedEventKindsInvalid_InvalidKeyAndNothingStored() {
		List<List<String>> invalid = new java.util.ArrayList<>();
		invalid.add(null);
		invalid.add(Collections.emptyList());
		invalid.add(List.of("NOPE"));
		invalid.add(List.of("authorization_decision"));
		invalid.add(List.of(TraceConstants.EVENT_KIND_RUNTIME_EFFECT, TraceConstants.EVENT_KIND_RUNTIME_EFFECT));
		invalid.add(java.util.Arrays.asList(TraceConstants.EVENT_KIND_RUNTIME_EFFECT, null));

		for (List<String> kinds : invalid) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> registry.register(DOMAIN, PRODUCER, KID, jwk, kinds, 1000L, null, REGISTERED_BY, 1000L),
					String.valueOf(kinds));

			assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
			assertEquals(ProducerKeyRegistry.REASON_AUTHORIZED_EVENT_KINDS, ex.getReason());
		}
		assertTrue(registry.list(DOMAIN, null).isEmpty());
	}

	@Test
	void testRequireEventKindAuthorized_KindInScope_Passes() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, List.of(TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION),
				1000L, null, REGISTERED_BY, 1000L);
		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);

		registry.requireEventKindAuthorized(resolved, TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION);
	}

	@Test
	void testRequireEventKindAuthorized_KindOutOfScope_UnknownProducerKey() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, List.of(TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION),
				1000L, null, REGISTERED_BY, 1000L);
		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.requireEventKindAuthorized(resolved, TraceConstants.EVENT_KIND_CAPABILITY_INVOKED));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_EVENT_KIND_NOT_AUTHORIZED, ex.getReason());
	}

	@Test
	void testRequireEventKindAuthorized_LegacyRowWithoutAttribute_RejectsEveryKind() throws Exception {
		TraceProducerKeyEntry legacy = new TraceProducerKeyEntry();
		legacy.setDomainId(DOMAIN);
		legacy.setProducerId(PRODUCER);
		legacy.setKid(KID);
		legacy.setPublicKeyJwk(jwk);
		legacy.setValidFrom(new Date(1000L));
		legacy.setRegisteredBy(REGISTERED_BY);
		legacy.setCreationDate(new Date(1000L));
		store.insertProducerKey(legacy);
		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);

		assertTrue(resolved.getAuthorizedEventKinds().isEmpty());
		for (String kind : TraceConstants.EVENT_KINDS) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> registry.requireEventKindAuthorized(resolved, kind), kind);
			assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
			assertEquals(ProducerKeyRegistry.REASON_EVENT_KIND_NOT_AUTHORIZED, ex.getReason());
		}
	}

	@Test
	void testRevoke_KeepsAuthorizedEventKinds() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, List.of(TraceConstants.EVENT_KIND_RUNTIME_EFFECT), 1000L, null,
				REGISTERED_BY, 1000L);

		TraceProducerKeyEntry revoked = registry.revoke(DOMAIN, PRODUCER, KID, 5000L).get();

		assertEquals(List.of(TraceConstants.EVENT_KIND_RUNTIME_EFFECT), revoked.getAuthorizedEventKinds());
	}

	// -- list -----------------------------------------------------------------------------------

	@Test
	void testList_FiltersByProducerId() {
		registry.register(DOMAIN, PRODUCER, "kid-a", jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		registry.register(DOMAIN, "other-producer", "kid-b", jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);

		List<TraceProducerKeyEntry> filtered = registry.list(DOMAIN, PRODUCER);

		assertEquals(Collections.singletonList("kid-a"),
				filtered.stream().map(TraceProducerKeyEntry::getKid).collect(java.util.stream.Collectors.toList()));
	}

	private static String repeat(char c, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

	// -- request key memo ------------------------------------------------------------------

	/** Wires {@link #registry} over a store that counts {@code findProducerKey} calls. */
	private AtomicInteger countKeyReads() {
		AtomicInteger reads = new AtomicInteger();
		InMemoryTraceStore counting = new InMemoryTraceStore() {
			@Override
			public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
				reads.incrementAndGet();
				return store.findProducerKey(domainId, producerId, kid);
			}
		};
		setField(registry, "traceStore", counting);
		return reads;
	}

	@Test
	void testRequestKeys_SameKeyTwice_OneReadOneParse() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		AtomicInteger reads = countKeyReads();
		ProducerKeyRegistry.KeyResolver keys = registry.newRequestKeys();

		ProducerKeyRegistry.ResolvedKey first = keys.resolve(DOMAIN, PRODUCER, KID, 5000L);
		ProducerKeyRegistry.ResolvedKey second = keys.resolve(DOMAIN, PRODUCER, KID, 5000L);

		assertEquals(1, reads.get());
		assertEquals(KID, second.getKey().getKid());
		assertSame(first.getVerificationKey(), second.getVerificationKey());
	}

	@Test
	void testRequestKeys_KeptKey_ValidityJudgedAtEachCallersInstant() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, 9000L, REGISTERED_BY, 1000L);
		AtomicInteger reads = countKeyReads();
		ProducerKeyRegistry.KeyResolver keys = registry.newRequestKeys();

		keys.resolve(DOMAIN, PRODUCER, KID, 5000L);
		TraceValidationException expired = assertThrows(TraceValidationException.class,
				() -> keys.resolve(DOMAIN, PRODUCER, KID, 9000L));
		TraceValidationException early = assertThrows(TraceValidationException.class,
				() -> keys.resolve(DOMAIN, PRODUCER, KID, 999L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, expired.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_EXPIRED, expired.getReason());
		assertEquals(ProducerKeyRegistry.REASON_NOT_YET_VALID, early.getReason());
		assertEquals(1, reads.get());
	}

	@Test
	void testRequestKeys_FailingKey_NotKeptAndReadAgain() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		registry.revoke(DOMAIN, PRODUCER, KID, 5000L);
		AtomicInteger reads = countKeyReads();
		ProducerKeyRegistry.KeyResolver keys = registry.newRequestKeys();

		for (int i = 0; i < 3; i++) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> keys.resolve(DOMAIN, PRODUCER, KID, 6000L));
			assertEquals(ProducerKeyRegistry.REASON_REVOKED, ex.getReason());
		}
		TraceValidationException unknown = assertThrows(TraceValidationException.class,
				() -> keys.resolve(DOMAIN, PRODUCER, "kid-unknown", 6000L));

		assertEquals(ProducerKeyRegistry.REASON_KEY_NOT_REGISTERED, unknown.getReason());
		assertEquals(4, reads.get());
	}

	@Test
	void testRequestKeys_UnusableStoredKey_SameReasonsAndOrderAsResolveForVerification() throws Exception {
		Map<String, String> badJwk = new LinkedHashMap<>();
		badJwk.put("kty", "OKP");
		badJwk.put("crv", "Ed25519");
		badJwk.put("x", "not-valid-base64url!!");
		TraceProducerKeyEntry key = new TraceProducerKeyEntry();
		key.setDomainId(DOMAIN);
		key.setProducerId(PRODUCER);
		key.setKid(KID);
		key.setPublicKeyJwk(badJwk);
		key.setValidFrom(new Date(1000L));
		key.setValidUntil(new Date(9000L));
		key.setRegisteredBy(REGISTERED_BY);
		key.setCreationDate(new Date(1000L));
		store.insertProducerKey(key);
		AtomicInteger reads = countKeyReads();
		ProducerKeyRegistry.KeyResolver keys = registry.newRequestKeys();

		for (long now : new long[] { 5000L, 9000L, 5000L }) {
			TraceValidationException direct = assertThrows(TraceValidationException.class,
					() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, now));
			TraceValidationException memo = assertThrows(TraceValidationException.class,
					() -> keys.resolve(DOMAIN, PRODUCER, KID, now));
			assertEquals(direct.getErrorId(), memo.getErrorId());
			assertEquals(direct.getReason(), memo.getReason());
		}
		assertEquals(6, reads.get(), "an unusable key is never kept");
	}

	@Test
	void testRequestKeys_StoreFailure_PropagatesAndIsNotKept() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		AtomicInteger reads = new AtomicInteger();
		InMemoryTraceStore failingOnce = new InMemoryTraceStore() {
			@Override
			public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
				if (reads.incrementAndGet() == 1) {
					throw new TraceStorageException("read_failed", "injected");
				}
				return store.findProducerKey(domainId, producerId, kid);
			}
		};
		setField(registry, "traceStore", failingOnce);
		ProducerKeyRegistry.KeyResolver keys = registry.newRequestKeys();

		assertThrows(TraceStorageException.class, () -> keys.resolve(DOMAIN, PRODUCER, KID, 5000L));
		keys.resolve(DOMAIN, PRODUCER, KID, 5000L);
		keys.resolve(DOMAIN, PRODUCER, KID, 5000L);

		assertEquals(2, reads.get());
	}

	@Test
	void testRequestKeys_ConcurrentCallers_OneRead() throws Exception {
		registry.register(DOMAIN, PRODUCER, KID, jwk, ALL_KINDS, 1000L, null, REGISTERED_BY, 1000L);
		AtomicInteger reads = new AtomicInteger();
		InMemoryTraceStore slow = new InMemoryTraceStore() {
			@Override
			public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
				reads.incrementAndGet();
				try {
					Thread.sleep(50);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				return store.findProducerKey(domainId, producerId, kid);
			}
		};
		setField(registry, "traceStore", slow);
		ProducerKeyRegistry.KeyResolver keys = registry.newRequestKeys();
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<ProducerKeyRegistry.ResolvedKey>> futures = new ArrayList<>();
			for (int i = 0; i < 16; i++) {
				futures.add(pool.submit(() -> {
					start.await();
					return keys.resolve(DOMAIN, PRODUCER, KID, 5000L);
				}));
			}
			start.countDown();
			for (Future<ProducerKeyRegistry.ResolvedKey> future : futures) {
				assertEquals(KID, future.get(10, TimeUnit.SECONDS).getKey().getKid());
			}
		} finally {
			pool.shutdownNow();
		}

		assertEquals(1, reads.get());
	}

}
