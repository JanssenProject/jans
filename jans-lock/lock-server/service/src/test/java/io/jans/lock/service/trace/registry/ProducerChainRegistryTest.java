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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.store.InMemoryTraceStore;

/**
 * Tests for {@link ProducerChainRegistry}: pre-registration lookup, the design decision D-5
 * genesis-hash matrix, and create-only chain registration.
 */
class ProducerChainRegistryTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String INSTANCE = "instance-1";

	private static final String CHAIN = "chain-1";

	private static final String REGISTERED_BY = "2200.abcd";

	private InMemoryTraceStore store;

	private ProducerChainRegistry registry;

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		registry = new ProducerChainRegistry();
		setField(registry, "traceStore", store);
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

		TraceChainEntry found = registry.requireRegistered(chainIdentity());

		assertEquals(chainIdentity(), toChainIdentity(found));
	}

	// -- register ---------------------------------------------------------------------------------

	@Test
	void testRegister_NewChain_Succeeds() {
		TraceChainEntry registration = registry.register(chainIdentity(), REGISTERED_BY, 1000L);

		assertEquals(chainIdentity(), toChainIdentity(registration));
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
	void testRegister_ProducerIdBadFormat_InvalidRequest() {
		ChainIdentity id = new ChainIdentity(DOMAIN, "-bad producer", INSTANCE, CHAIN);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.register(id, REGISTERED_BY, 1000L));

		assertEquals(TraceErrorResponseType.INVALID_REQUEST, ex.getErrorId());
		assertEquals(ProducerChainRegistry.REASON_FORMAT_PRODUCER_ID, ex.getReason());
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
		registry.register(new ChainIdentity(DOMAIN, "other-producer", INSTANCE, CHAIN), REGISTERED_BY, 1000L);

		List<TraceChainEntry> filtered = registry.list(DOMAIN, PRODUCER);

		assertEquals(1, filtered.size());
		assertTrue(filtered.get(0).getProducerId().equals(PRODUCER));
	}

	// -- register: genesis kind (task 30) -------------------------------------------------------------

	@Test
	void testRegister_AdminRegistration_GenesisKindPreRegistered() {
		TraceChainEntry registration = registry.register(chainIdentity(), REGISTERED_BY, 1000L);

		assertEquals(TraceChainEntry.GENESIS_KIND_PRE_REGISTERED, registration.getGenesisKind());
		assertEquals(TraceChainEntry.GENESIS_KIND_PRE_REGISTERED,
				store.findChain(chainIdentity()).orElseThrow().getGenesisKind());
	}

	@Test
	void testGenesisKind_RowStoredWithoutAttribute_ReadsAsPreRegistered() {
		TraceChainEntry legacy = new TraceChainEntry();

		assertEquals(TraceChainEntry.GENESIS_KIND_PRE_REGISTERED, legacy.getGenesisKind());
	}

	// -- requireRegisteredOrObserveGenesis (task 30) ---------------------------------------------------

	@Test
	void testObserveGenesis_UnregisteredGenesis_RegistersFirstObserved() {
		TraceChainEntry chain = registry.requireRegisteredOrObserveGenesis(chainIdentity(), 1L,
				TraceConstants.ZERO_HASH, REGISTERED_BY, 2000L);

		assertEquals(TraceChainEntry.GENESIS_KIND_FIRST_OBSERVED, chain.getGenesisKind());
		assertEquals(REGISTERED_BY, chain.getRegisteredBy());
		assertEquals(2000L, chain.getRegisteredAtMs());
		TraceChainEntry stored = store.findChain(chainIdentity()).orElseThrow();
		assertEquals(TraceChainEntry.GENESIS_KIND_FIRST_OBSERVED, stored.getGenesisKind());
	}

	@Test
	void testObserveGenesis_AlreadyRegistered_ReturnsExistingUnchanged() {
		registry.register(chainIdentity(), "2200.admin", 1000L);

		TraceChainEntry chain = registry.requireRegisteredOrObserveGenesis(chainIdentity(), 1L,
				TraceConstants.ZERO_HASH, REGISTERED_BY, 2000L);

		assertEquals(TraceChainEntry.GENESIS_KIND_PRE_REGISTERED, chain.getGenesisKind());
		assertEquals("2200.admin", chain.getRegisteredBy());
		assertEquals(1, store.findChains(DOMAIN, null).size());
	}

	@Test
	void testObserveGenesis_AlreadyRegisteredNonGenesisPosition_ReturnsExisting() {
		registry.register(chainIdentity(), REGISTERED_BY, 1000L);

		TraceChainEntry chain = registry.requireRegisteredOrObserveGenesis(chainIdentity(), 7L,
				"sha256:" + repeat('a', 64), REGISTERED_BY, 2000L);

		assertEquals(chainIdentity(), toChainIdentity(chain));
	}

	@Test
	void testObserveGenesis_UnregisteredNonGenesis_ChainNotRegisteredNothingStored() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.requireRegisteredOrObserveGenesis(chainIdentity(), 2L, "sha256:" + repeat('a', 64),
						REGISTERED_BY, 2000L));

		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
		assertTrue(store.findChains(DOMAIN, null).isEmpty());
	}

	@Test
	void testObserveGenesis_UnregisteredGenesisWithoutSentinel_InvalidGenesis() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.requireRegisteredOrObserveGenesis(chainIdentity(), 1L, "sha256:" + repeat('a', 64),
						REGISTERED_BY, 2000L));

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
		assertTrue(store.findChains(DOMAIN, null).isEmpty());
	}

	@Test
	void testObserveGenesis_UnregisteredNonGenesisWithSentinel_InvalidGenesis() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> registry.requireRegisteredOrObserveGenesis(chainIdentity(), 3L, TraceConstants.ZERO_HASH,
						REGISTERED_BY, 2000L));

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
		assertTrue(store.findChains(DOMAIN, null).isEmpty());
	}

	@Test
	void testObserveGenesis_ConcurrentRegistrationWins_CountsAsRegistered() {
		InMemoryTraceStore racingStore = new InMemoryTraceStore() {
			private boolean raced;

			@Override
			public Optional<TraceChainEntry> findChain(ChainIdentity id) {
				if (!raced) {
					raced = true;
					TraceChainEntry competitor = new TraceChainEntry();
					competitor.setDomainId(id.getDomainId());
					competitor.setProducerId(id.getProducerId());
					competitor.setProducerInstanceId(id.getProducerInstanceId());
					competitor.setProducerChainId(id.getProducerChainId());
					competitor.setRegisteredBy("2200.other-node");
					competitor.setGenesisKind(TraceChainEntry.GENESIS_KIND_FIRST_OBSERVED);
					competitor.setCreationDate(new Date(1500L));
					try {
						insertChain(competitor);
					} catch (DuplicateEntryException ex) {
						throw new IllegalStateException(ex);
					}
					return Optional.empty();
				}
				return super.findChain(id);
			}
		};
		setField(registry, "traceStore", racingStore);

		TraceChainEntry chain = registry.requireRegisteredOrObserveGenesis(chainIdentity(), 1L,
				TraceConstants.ZERO_HASH, REGISTERED_BY, 2000L);

		assertEquals("2200.other-node", chain.getRegisteredBy());
		assertEquals(1, racingStore.findChains(DOMAIN, null).size());
	}

	private static ChainIdentity toChainIdentity(TraceChainEntry entity) {
		return new ChainIdentity(entity.getDomainId(), entity.getProducerId(), entity.getProducerInstanceId(),
				entity.getProducerChainId());
	}

	private static String repeat(char c, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

}
