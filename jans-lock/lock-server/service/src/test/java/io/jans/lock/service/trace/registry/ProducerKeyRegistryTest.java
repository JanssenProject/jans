/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.crypto.Ed25519TestKeys;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceCryptoException;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.parse.TraceValidationException;
import io.jans.lock.service.trace.store.InMemoryTraceStore;

/**
 * Tests for {@link ProducerKeyRegistry}: the design decision D-4 validity matrix, create-only
 * registration, idempotent revocation and the "stored key unusable" data-error path.
 */
class ProducerKeyRegistryTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "cedarling-fleet-1/1.0.0";

	private static final String KID = "kid-1";

	private static final String REGISTERED_BY = "2200.abcd";

	private InMemoryTraceStore store;

	private ProducerKeyRegistry registry;

	private Map<String, String> jwk;

	@BeforeAll
	static void installProvider() {
		Ed25519TestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		registry = new ProducerKeyRegistry();
		registry.setTraceStore(store);
		registry.setLog(LoggerFactory.getLogger(ProducerKeyRegistry.class));

		KeyPair keyPair = Ed25519TestKeys.generateKeyPair();
		jwk = Ed25519TestKeys.toJwk(keyPair.getPublic());
	}

	// -- registration ---------------------------------------------------------------------------

	@Test
	void testRegister_NewKey_Succeeds() {
		ProducerKey key = registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);

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
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);

		TraceConflictException ex = assertThrows(TraceConflictException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.KEY_ALREADY_EXISTS, ex.getErrorId());
	}

	@Test
	void testRegister_ValidUntilBeforeValidFrom_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, jwk, 2000L, 1000L, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_VALIDITY_WINDOW, ex.getReason());
	}

	@Test
	void testRegister_ValidUntilEqualsValidFrom_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, 1000L, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_VALIDITY_WINDOW, ex.getReason());
	}

	@Test
	void testRegister_ProducerIdBadFormat_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, "not-a-valid-producer-id", KID, jwk, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_PRODUCER_ID_FORMAT, ex.getReason());
	}

	@Test
	void testRegister_EmptyKid_InvalidKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, "", jwk, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_KID_EMPTY, ex.getReason());
	}

	@Test
	void testRegister_KidTooLong_InvalidKey() {
		String longKid = repeat('k', 256);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(DOMAIN, PRODUCER, longKid, jwk, 1000L, null, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_KID_LENGTH, ex.getReason());
	}

	@Test
	void testRegister_MalformedJwk_PropagatesTraceCryptoException() {
		Map<String, String> badJwk = new LinkedHashMap<>();
		badJwk.put("kty", "RSA");

		assertThrows(TraceCryptoException.class,
				() -> registry.register(DOMAIN, PRODUCER, KID, badJwk, 1000L, null, REGISTERED_BY, 1000L));
	}

	// -- revocation -------------------------------------------------------------------------------

	@Test
	void testRevoke_UnknownKey_ReturnsEmpty() {
		Optional<ProducerKey> result = registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		assertFalse(result.isPresent());
	}

	@Test
	void testRevoke_KnownKey_SetsRevokedAt() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);

		Optional<ProducerKey> result = registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		assertTrue(result.isPresent());
		assertEquals(Long.valueOf(5000L), result.get().getRevokedAtMs());
	}

	@Test
	void testRevoke_Twice_IdempotentSameRevokedAt() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);
		registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		Optional<ProducerKey> result = registry.revoke(DOMAIN, PRODUCER, KID, 9000L);

		assertTrue(result.isPresent());
		assertEquals(Long.valueOf(5000L), result.get().getRevokedAtMs());
	}

	// -- resolveForVerification: design decision D-4 validity matrix ----------------------------

	@Test
	void testResolveForVerification_UnknownKey_UnknownProducerKey() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_KEY_NOT_REGISTERED, ex.getReason());
	}

	@Test
	void testResolveForVerification_BeforeValidFrom_NotYetValid() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 5000L, null, REGISTERED_BY, 1000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 4999L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_NOT_YET_VALID, ex.getReason());
	}

	@Test
	void testResolveForVerification_InsideWindow_Resolves() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, 9000L, REGISTERED_BY, 1000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L);

		assertEquals(KID, resolved.getKey().getKid());
		assertTrue(resolved.getPublicKey() != null);
	}

	@Test
	void testResolveForVerification_NoValidUntil_ResolvesFarInTheFuture() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID,
				Long.MAX_VALUE / 2);

		assertEquals(KID, resolved.getKey().getKid());
	}

	@Test
	void testResolveForVerification_AtValidFrom_Resolves() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, 9000L, REGISTERED_BY, 1000L);

		ProducerKeyRegistry.ResolvedKey resolved = registry.resolveForVerification(DOMAIN, PRODUCER, KID, 1000L);

		assertEquals(KID, resolved.getKey().getKid());
	}

	@Test
	void testResolveForVerification_AfterValidUntil_Expired() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, 9000L, REGISTERED_BY, 1000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 9000L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_EXPIRED, ex.getReason());
	}

	@Test
	void testResolveForVerification_RevokedInThePast_Revoked() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);
		registry.revoke(DOMAIN, PRODUCER, KID, 5000L);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 6000L));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_REVOKED, ex.getReason());
	}

	@Test
	void testResolveForVerification_RevokedInTheFuture_StillValidNow() {
		registry.register(DOMAIN, PRODUCER, KID, jwk, 1000L, null, REGISTERED_BY, 1000L);
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
		ProducerKey key = new ProducerKey(DOMAIN, PRODUCER, KID, badJwk, 1000L, null, null, REGISTERED_BY, 1000L);
		store.insertProducerKey(key);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.resolveForVerification(DOMAIN, PRODUCER, KID, 5000L));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals(ProducerKeyRegistry.REASON_STORED_KEY_UNUSABLE, ex.getReason());
	}

	// -- list -----------------------------------------------------------------------------------

	@Test
	void testList_FiltersByProducerId() {
		registry.register(DOMAIN, PRODUCER, "kid-a", jwk, 1000L, null, REGISTERED_BY, 1000L);
		registry.register(DOMAIN, "other-producer/2.0.0", "kid-b", jwk, 1000L, null, REGISTERED_BY, 1000L);

		List<ProducerKey> filtered = registry.list(DOMAIN, PRODUCER);

		assertEquals(Collections.singletonList("kid-a"),
				filtered.stream().map(ProducerKey::getKid).collect(java.util.stream.Collectors.toList()));
	}

	private static String repeat(char c, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

}
