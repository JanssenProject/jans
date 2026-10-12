/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.Entry;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.EntryStatus;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;

/**
 * Tests for {@link TraceReceiptChain}: sequential allocation and linkage, cross-node contention on
 * the same sequence, retry exhaustion, and the VOID-tombstone-then-continue case (design §9, TRACE
 * MVP design decision D-8; task 18 acceptance criteria).
 */
class TraceReceiptChainTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "producer-1/1.0.0";

	private static final String NODE_ID = "node-1";

	private InMemoryTraceStore store;

	private TraceConfiguration traceConfiguration;

	private AppConfiguration appConfiguration;

	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		traceConfiguration = new TraceConfiguration();
		appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);
	}

	@AfterEach
	void tearDown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	private TraceReceiptChain newChain(InMemoryTraceStore backingStore) {
		TraceReceiptChain chain = new TraceReceiptChain();
		setField(chain, "log", LoggerFactory.getLogger(TraceReceiptChain.class));
		setField(chain, "store", backingStore);
		setField(chain, "appConfiguration", appConfiguration);
		return chain;
	}

	private void insertMatchingRecord(String recordId, String contentDigest) throws DuplicateEntryException {
		StoredTraceRecord record = StoredTraceRecordFactory.builder().domainId(DOMAIN).producerId(PRODUCER)
				.recordId(recordId).contentDigest(contentDigest).build();
		store.insertRecord(record);
	}

	// -- sequential claims and linkage -----------------------------------------------------------

	@Test
	void testClaim_Sequential_LinkedAndVerifyDomainOk() throws DuplicateEntryException {
		TraceReceiptChain chain = newChain(store);

		ReceiptClaim claim1 = chain.claim(DOMAIN, PRODUCER, "rec-1", "rec-1-key", "sha256:" + digest('1'), 1_000L,
				NODE_ID);
		assertEquals(1L, claim1.getSeq());
		assertEquals(TraceConstants.ZERO_HASH, claim1.getPrevReceiptHash());
		insertMatchingRecord("rec-1", "sha256:" + digest('1'));
		chain.commit(DOMAIN, claim1.getSeq());

		ReceiptClaim claim2 = chain.claim(DOMAIN, PRODUCER, "rec-2", "rec-2-key", "sha256:" + digest('2'), 2_000L,
				NODE_ID);
		assertEquals(2L, claim2.getSeq());
		assertEquals(claim1.getReceiptHash(), claim2.getPrevReceiptHash());
		insertMatchingRecord("rec-2", "sha256:" + digest('2'));
		chain.commit(DOMAIN, claim2.getSeq());

		ReceiptClaim claim3 = chain.claim(DOMAIN, PRODUCER, "rec-3", "rec-3-key", "sha256:" + digest('3'), 3_000L,
				NODE_ID);
		assertEquals(3L, claim3.getSeq());
		assertEquals(claim2.getReceiptHash(), claim3.getPrevReceiptHash());
		insertMatchingRecord("rec-3", "sha256:" + digest('3'));
		chain.commit(DOMAIN, claim3.getSeq());

		ChainVerificationReport report = chain.verifyDomain(DOMAIN, 100);
		assertTrue(report.isOk());
		assertEquals(3L, report.getHeadSequence());
		assertEquals(3, report.getEntries().size());
		for (Entry entry : report.getEntries()) {
			assertTrue(entry.isOk(), "entry " + entry + " expected OK");
			assertEquals(TraceReceiptState.COMMITTED, entry.getState());
		}
	}

	@Test
	void testVerifyDomain_EmptyDomain_OkWithZeroHead() {
		TraceReceiptChain chain = newChain(store);

		ChainVerificationReport report = chain.verifyDomain(DOMAIN, 100);

		assertTrue(report.isOk());
		assertEquals(0L, report.getHeadSequence());
		assertTrue(report.getEntries().isEmpty());
	}

	// -- cross-node contention on the same sequence ------------------------------------------------

	@Test
	void testClaim_ConcurrentContentionAcrossNodes_BothSucceedWithDistinctSequences() throws Exception {
		CountDownLatch rendezvous = new CountDownLatch(2);
		store.setInsertReceiptBarrier(rendezvous, 5, TimeUnit.SECONDS);

		TraceReceiptChain chainNodeA = newChain(store);
		TraceReceiptChain chainNodeB = newChain(store);

		executor = Executors.newFixedThreadPool(2);
		Callable<ReceiptClaim> taskA = () -> chainNodeA.claim(DOMAIN, PRODUCER, "rec-a", "rec-a-key",
				"sha256:" + digest('a'), 1_000L, "node-a");
		Callable<ReceiptClaim> taskB = () -> chainNodeB.claim(DOMAIN, PRODUCER, "rec-b", "rec-b-key",
				"sha256:" + digest('b'), 1_000L, "node-b");

		Future<ReceiptClaim> futureA = executor.submit(taskA);
		Future<ReceiptClaim> futureB = executor.submit(taskB);

		ReceiptClaim claimA = futureA.get(5, TimeUnit.SECONDS);
		ReceiptClaim claimB = futureB.get(5, TimeUnit.SECONDS);

		store.setInsertReceiptBarrier(null, 0, TimeUnit.MILLISECONDS);

		Set<Long> sequences = new HashSet<>();
		sequences.add(claimA.getSeq());
		sequences.add(claimB.getSeq());
		assertEquals(2, sequences.size());
		assertTrue(sequences.contains(1L));
		assertTrue(sequences.contains(2L));
		assertEquals(2L, store.findReceiptHead(DOMAIN).get().getReceiptSequence());
	}

	// -- retry exhaustion --------------------------------------------------------------------------

	@Test
	void testClaim_AlwaysContended_RetryExhausted_ThrowsTraceStorageException() {
		traceConfiguration.setReceiptAllocationRetryLimit(3);
		AlwaysDuplicateReceiptStore alwaysDuplicateStore = new AlwaysDuplicateReceiptStore();
		TraceReceiptChain chain = newChain(alwaysDuplicateStore);

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> chain.claim(DOMAIN, PRODUCER, "rec-x", "rec-x-key", "sha256:" + digest('x'), 1_000L, NODE_ID));

		assertEquals("receipt_allocation_exhausted", ex.getReason());
	}

	// -- VOID tombstone then continue ----------------------------------------------------------------

	@Test
	void testVoidClaim_TombstoneKeepsChainDense_NextClaimLinksToVoidedHash() throws DuplicateEntryException {
		TraceReceiptChain chain = newChain(store);

		ReceiptClaim claim1 = chain.claim(DOMAIN, PRODUCER, "rec-1", "rec-1-key", "sha256:" + digest('1'), 1_000L,
				NODE_ID);
		insertMatchingRecord("rec-1", "sha256:" + digest('1'));
		chain.commit(DOMAIN, claim1.getSeq());

		ReceiptClaim claim2 = chain.claim(DOMAIN, PRODUCER, "rec-2", "rec-2-key", "sha256:" + digest('2'), 2_000L,
				NODE_ID);
		// No matching record row inserted for rec-2: the identity race's loser, voided (D-8 step 5).
		chain.voidClaim(DOMAIN, claim2.getSeq());

		ReceiptClaim claim3 = chain.claim(DOMAIN, PRODUCER, "rec-3", "rec-3-key", "sha256:" + digest('3'), 3_000L,
				NODE_ID);
		assertEquals(3L, claim3.getSeq());
		assertEquals(claim2.getReceiptHash(), claim3.getPrevReceiptHash(),
				"prev hash must link to the voided position's hash, not skip it");
		assertNotEquals(claim1.getReceiptHash(), claim2.getReceiptHash());
		insertMatchingRecord("rec-3", "sha256:" + digest('3'));
		chain.commit(DOMAIN, claim3.getSeq());

		ChainVerificationReport report = chain.verifyDomain(DOMAIN, 100);
		assertTrue(report.isOk(), "VOID tombstone must not break chain integrity: " + report);
		assertEquals(3L, report.getHeadSequence());
		List<Entry> entries = report.getEntries();
		assertEquals(3, entries.size());
		assertEquals(TraceReceiptState.COMMITTED, entries.get(0).getState());
		assertEquals(TraceReceiptState.VOID, entries.get(1).getState());
		assertEquals(EntryStatus.OK, entries.get(1).getStatus());
		assertEquals(TraceReceiptState.COMMITTED, entries.get(2).getState());
	}

	private static String digest(char c) {
		char[] chars = new char[64];
		java.util.Arrays.fill(chars, c);
		return new String(chars);
	}

	/** Every {@code insertReceipt} call loses the identity race, forcing retry exhaustion. */
	private static final class AlwaysDuplicateReceiptStore extends InMemoryTraceStore {

		@Override
		public void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException {
			throw new DuplicateEntryException("forced-contention");
		}

	}

}
