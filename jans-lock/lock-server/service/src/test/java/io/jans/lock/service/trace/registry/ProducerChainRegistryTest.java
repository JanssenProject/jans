/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainRegistration;
import io.jans.lock.service.trace.store.InMemoryTraceStore;

/**
 * Tests for {@link ProducerChainRegistry}: pre-registration lookup, the design decision D-5
 * genesis-hash matrix, and create-only chain registration.
 */
class ProducerChainRegistryTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "cedarling-fleet-1/1.0.0";

	private static final String INSTANCE = "instance-1";

	private static final String CHAIN = "chain-1";

	private static final String REGISTERED_BY = "2200.abcd";

	private InMemoryTraceStore store;

	private ProducerChainRegistry registry;

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		registry = new ProducerChainRegistry();
		registry.setTraceStore(store);
	}

	private static ChainIdentity chainIdentity() {
		return new ChainIdentity(DOMAIN, PRODUCER, INSTANCE, CHAIN);
	}

	// -- requireRegistered ------------------------------------------------------------------------

	@Test
	void testRequireRegistered_NotRegistered_ChainNotRegistered() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.requireRegistered(chainIdentity()));

		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
	}

	@Test
	void testRequireRegistered_Registered_ReturnsRegistration() {
		registry.register(chainIdentity(), REGISTERED_BY, 1000L);

		ChainRegistration found = registry.requireRegistered(chainIdentity());

		assertEquals(chainIdentity(), found.getChainIdentity());
	}

	// -- register ---------------------------------------------------------------------------------

	@Test
	void testRegister_NewChain_Succeeds() {
		ChainRegistration registration = registry.register(chainIdentity(), REGISTERED_BY, 1000L);

		assertEquals(chainIdentity(), registration.getChainIdentity());
		assertEquals(1000L, registration.getRegisteredAtMs());
		assertEquals(REGISTERED_BY, registration.getRegisteredBy());
	}

	@Test
	void testRegister_SameChainTwice_ChainAlreadyExists() {
		registry.register(chainIdentity(), REGISTERED_BY, 1000L);

		TraceConflictException ex = assertThrows(TraceConflictException.class,
				() -> registry.register(chainIdentity(), REGISTERED_BY, 2000L));

		assertEquals(TraceErrorResponseType.CHAIN_ALREADY_EXISTS, ex.getErrorId());
	}

	@Test
	void testRegister_EmptyProducerInstanceId_InvalidRequest() {
		ChainIdentity id = new ChainIdentity(DOMAIN, PRODUCER, "", CHAIN);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(id, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_REQUEST, ex.getErrorId());
	}

	@Test
	void testRegister_ProducerChainIdTooLong_InvalidRequest() {
		ChainIdentity id = new ChainIdentity(DOMAIN, PRODUCER, INSTANCE, repeat('c', 256));

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(id, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_REQUEST, ex.getErrorId());
	}

	// -- checkGenesis: design decision D-5 matrix --------------------------------------------------

	@Test
	void testCheckGenesis_Seq1WithSentinel_Passes() {
		assertDoesNotThrow(() -> registry.checkGenesis(1L, TraceConstants.ZERO_HASH));
	}

	@Test
	void testCheckGenesis_Seq1WithoutSentinel_InvalidGenesis() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.checkGenesis(1L, "sha256:" + repeat('a', 64)));

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
		assertEquals(ProducerChainRegistry.REASON_GENESIS_HASH, ex.getReason());
	}

	@Test
	void testCheckGenesis_Seq2WithSentinel_InvalidGenesis() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.checkGenesis(2L, TraceConstants.ZERO_HASH));

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
		assertEquals(ProducerChainRegistry.REASON_SENTINEL_ON_NON_GENESIS, ex.getReason());
	}

	@Test
	void testCheckGenesis_Seq2WithoutSentinel_Passes() {
		assertDoesNotThrow(() -> registry.checkGenesis(2L, "sha256:" + repeat('a', 64)));
	}

	// -- list ---------------------------------------------------------------------------------------

	@Test
	void testList_FiltersByProducerId() {
		registry.register(chainIdentity(), REGISTERED_BY, 1000L);
		registry.register(new ChainIdentity(DOMAIN, "other-producer/2.0.0", INSTANCE, CHAIN), REGISTERED_BY, 1000L);

		List<ChainRegistration> filtered = registry.list(DOMAIN, PRODUCER);

		assertEquals(1, filtered.size());
		assertTrue(filtered.get(0).getChainIdentity().getProducerId().equals(PRODUCER));
	}

	private static String repeat(char c, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

}
