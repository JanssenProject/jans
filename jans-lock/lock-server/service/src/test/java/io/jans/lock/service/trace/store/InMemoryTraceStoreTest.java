/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.ReceiptRow;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.VerificationResult;

/**
 * Runs the shared {@link TraceStoreContractTest} suite against {@link InMemoryTraceStore}, plus
 * tests for its test-only hooks ({@code failNext*}, the insert barrier, {@code snapshot()}) that
 * are specific to this implementation rather than part of the {@link TraceStore} contract.
 *
 * @author Yuriy Movchan
 */
class InMemoryTraceStoreTest extends TraceStoreContractTest {

	@Override
	protected TraceStore createStore() {
		return new InMemoryTraceStore();
	}

	private StoredTraceRecord newRecord(String producerId, String recordId, long seq) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, producerId, recordId);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, "authority-1", "exec-1");
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, producerId, "instance-1", "chain-1");
		ChainPosition chainPosition = new ChainPosition(chainIdentity, seq);
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519");
		ReceiptEntry receipt = new ReceiptEntry(seq, 2000L + seq, TraceConstants.ZERO_HASH,
				"sha256:" + String.format("%064d", seq));
		return new StoredTraceRecord(identity, "{\"trace\":{}}", "sha256:" + String.format("%064d", seq), verification,
				receipt, new IngestionFlags(false, false, false, false), execution, chainPosition,
				TraceConstants.ZERO_HASH, java.util.Collections.emptyList(), java.util.Collections.emptyList(),
				TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 3000L + seq);
	}

	@Test
	void testFailNextInsertRecord_throwsOnce_thenSucceeds() throws Exception {
		InMemoryTraceStore inMemoryStore = (InMemoryTraceStore) store;
		inMemoryStore.failNextInsertRecord();

		StoredTraceRecord record = newRecord("producer-1", "record-1", 1L);
		assertThrows(TraceStorageException.class, () -> inMemoryStore.insertRecord(record));
		assertTrue(inMemoryStore.snapshot().isEmpty(), "nothing must become visible after the forced failure");

		inMemoryStore.insertRecord(record);
		assertEquals(1, inMemoryStore.snapshot().size());
	}

	@Test
	void testFailNextInsertReceipt_throwsOnce_thenSucceeds() throws Exception {
		InMemoryTraceStore inMemoryStore = (InMemoryTraceStore) store;
		inMemoryStore.failNextInsertReceipt();

		ReceiptRow row = new ReceiptRow(DOMAIN, 1L, 1000L, "producer-1", "record-1", "rec-key",
				"sha256:" + String.format("%064d", 1), TraceConstants.ZERO_HASH,
				"sha256:" + String.format("%064d", 1), TraceReceiptState.PENDING, "node-1");

		assertThrows(TraceStorageException.class, () -> inMemoryStore.insertReceipt(row));
		assertFalse(inMemoryStore.findReceipt(DOMAIN, 1L).isPresent());

		inMemoryStore.insertReceipt(row);
		assertTrue(inMemoryStore.findReceipt(DOMAIN, 1L).isPresent());
	}

	@Test
	void testFailNextUpdateReceiptState_throwsOnce_thenSucceeds() throws Exception {
		InMemoryTraceStore inMemoryStore = (InMemoryTraceStore) store;
		ReceiptRow row = new ReceiptRow(DOMAIN, 1L, 1000L, "producer-1", "record-1", "rec-key",
				"sha256:" + String.format("%064d", 1), TraceConstants.ZERO_HASH,
				"sha256:" + String.format("%064d", 1), TraceReceiptState.PENDING, "node-1");
		inMemoryStore.insertReceipt(row);

		inMemoryStore.failNextUpdateReceiptState();
		assertThrows(TraceStorageException.class,
				() -> inMemoryStore.updateReceiptState(DOMAIN, 1L, TraceReceiptState.COMMITTED));
		assertEquals(TraceReceiptState.PENDING, inMemoryStore.findReceipt(DOMAIN, 1L).get().getState());

		boolean updated = inMemoryStore.updateReceiptState(DOMAIN, 1L, TraceReceiptState.COMMITTED);
		assertTrue(updated);
		assertEquals(TraceReceiptState.COMMITTED, inMemoryStore.findReceipt(DOMAIN, 1L).get().getState());
	}

	@Test
	void testSnapshot_isIndependentOfLaterMutations() throws Exception {
		InMemoryTraceStore inMemoryStore = (InMemoryTraceStore) store;
		inMemoryStore.insertRecord(newRecord("producer-1", "record-1", 1L));

		List<StoredTraceRecord> snapshot = inMemoryStore.snapshot();
		inMemoryStore.insertRecord(newRecord("producer-2", "record-2", 1L));

		assertEquals(1, snapshot.size());
		assertEquals(2, inMemoryStore.snapshot().size());
	}

	@Test
	void testInsertRecordBarrier_concurrentInsertsOfDistinctIdentities_bothSucceed() throws Exception {
		InMemoryTraceStore inMemoryStore = (InMemoryTraceStore) store;
		CountDownLatch latch = new CountDownLatch(2);
		inMemoryStore.setInsertRecordBarrier(latch, 5, TimeUnit.SECONDS);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		AtomicInteger failures = new AtomicInteger(0);
		try {
			Runnable insertA = () -> {
				try {
					inMemoryStore.insertRecord(newRecord("producer-1", "record-a", 1L));
				} catch (Exception e) {
					failures.incrementAndGet();
				}
			};
			Runnable insertB = () -> {
				try {
					inMemoryStore.insertRecord(newRecord("producer-1", "record-b", 1L));
				} catch (Exception e) {
					failures.incrementAndGet();
				}
			};
			executor.submit(insertA);
			executor.submit(insertB);
			executor.shutdown();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		} finally {
			inMemoryStore.setInsertRecordBarrier(null, 0, TimeUnit.SECONDS);
		}

		assertEquals(0, failures.get());
		assertEquals(2, inMemoryStore.snapshot().size());
	}

	@Test
	void testInsertRecordBarrier_concurrentInsertsOfSameIdentity_oneWinsOneDuplicates() throws Exception {
		InMemoryTraceStore inMemoryStore = (InMemoryTraceStore) store;
		CountDownLatch latch = new CountDownLatch(2);
		inMemoryStore.setInsertRecordBarrier(latch, 5, TimeUnit.SECONDS);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		AtomicInteger duplicates = new AtomicInteger(0);
		AtomicInteger successes = new AtomicInteger(0);
		try {
			Runnable insert = () -> {
				try {
					inMemoryStore.insertRecord(newRecord("producer-1", "record-same", 1L));
					successes.incrementAndGet();
				} catch (DuplicateEntryException e) {
					duplicates.incrementAndGet();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			};
			executor.submit(insert);
			executor.submit(insert);
			executor.shutdown();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		} finally {
			inMemoryStore.setInsertRecordBarrier(null, 0, TimeUnit.SECONDS);
		}

		assertEquals(1, successes.get());
		assertEquals(1, duplicates.get());
		assertEquals(1, inMemoryStore.snapshot().size());
	}

}
