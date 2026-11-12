/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;

/**
 * ORM-specific behavior of the batch methods that the shared contract cannot observe: the size
 * guard, the single {@code persistBatch} call without reads, the exception mapping and the number
 * of round trips of the multi-get reads.
 *
 * @author Yuriy Movchan
 */
class OrmIngestBatchTest {

	private static final String DOMAIN = "default";

	private static final TraceKeys TRACE_KEYS = TraceTestCrypto.traceKeys();

	private PersistenceEntryManager manager;

	private OrmTraceStore store;

	@BeforeEach
	void setUp() {
		manager = FakeEntryManagerBehavior.create();
		store = OrmTraceStores.create(manager, TRACE_KEYS);
	}

	@Test
	void testBatchLimit_isTheOrmGuardAndHoldsTheDefaultBulkRequest() {
		assertEquals(1000, OrmTraceStore.MAX_INGEST_BATCH_ROWS);
		// one bulk request writes a receipt row and a record row per element
		assertTrue(2 * TraceConfiguration.DEFAULT_MAX_BULK_RECORDS <= OrmTraceStore.MAX_INGEST_BATCH_ROWS);
	}

	@Test
	void testInsertIngestBatch_aboveLimit_rejectedBeforeOrmCall() {
		List<StoredTraceRecord> records = records(1, OrmTraceStore.MAX_INGEST_BATCH_ROWS / 2 + 1);

		TraceStorageException e = assertThrows(TraceStorageException.class,
				() -> store.insertIngestBatch(receipts(records), records));

		assertEquals("batch_too_large", e.getReason());
		verify(manager, never()).persistBatch(any());
	}

	@Test
	void testInsertIngestBatch_oneOrmCallReceiptsFirstWithoutReads() throws Exception {
		List<StoredTraceRecord> records = records(1, 2);

		store.insertIngestBatch(receipts(records), records);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);
		verify(manager, times(1)).persistBatch(captor.capture());
		List<Object> written = captor.getValue();
		assertEquals(4, written.size());
		assertInstanceOf(TraceReceiptEntry.class, written.get(0));
		assertInstanceOf(TraceReceiptEntry.class, written.get(1));
		assertInstanceOf(TraceRecordEntry.class, written.get(2));
		assertInstanceOf(TraceRecordEntry.class, written.get(3));
		TraceReceiptEntry receipt = (TraceReceiptEntry) written.get(0);
		String receiptKey = TRACE_KEYS.receiptKey(DOMAIN, 1L);
		assertEquals(receiptKey, receipt.getId());
		assertEquals(TraceKeys.receiptDn(OrmTraceStores.BASE_DN, receiptKey), receipt.getDn());
		verify(manager, never()).contains(anyString(), any(Class.class));
		verify(manager, never()).find(anyString(), any(Class.class), any());
		verify(manager, never()).persist(any());
	}

	@Test
	void testInsertIngestBatch_duplicateCause_mappedWithKeyOfFailingRow() throws Exception {
		List<StoredTraceRecord> records = records(1, 2);
		String recordKey = TRACE_KEYS.recordKey(records.get(1).getIdentity());
		doThrow(new EntryPersistenceException("Failed to persist batch",
				new io.jans.orm.exception.operation.DuplicateEntryException(
						TraceKeys.recordDn(OrmTraceStores.BASE_DN, recordKey))))
				.when(manager).persistBatch(any());

		DuplicateEntryException e = assertThrows(DuplicateEntryException.class,
				() -> store.insertIngestBatch(receipts(records), records));

		assertEquals(recordKey, e.getKey());
		verify(manager, never()).contains(anyString(), any(Class.class));
	}

	@Test
	void testInsertIngestBatch_otherFailure_storageExceptionWithoutReprobe() {
		List<StoredTraceRecord> records = records(1, 2);
		doThrow(new EntryPersistenceException("connection lost")).when(manager).persistBatch(any());

		TraceStorageException e = assertThrows(TraceStorageException.class,
				() -> store.insertIngestBatch(receipts(records), records));

		assertEquals("persist_failed", e.getReason());
		verify(manager, never()).contains(anyString(), any(Class.class));
		assertFalse(store.findRecord(records.get(0).getIdentity()).isPresent());
		assertFalse(store.findReceipt(DOMAIN, 1L).isPresent());
	}

	@Test
	void testIsAtomicIngestSupported_followsTheManager() {
		assertTrue(store.isAtomicIngestSupported());
		assertFalse(OrmTraceStores.create(FakeEntryManagerBehavior.createWithoutAtomicBatch(), TRACE_KEYS)
				.isAtomicIngestSupported());
	}

	@Test
	void testFindRecords_oneOrmCall() throws Exception {
		List<StoredTraceRecord> records = records(1, 3);
		store.insertIngestBatch(receipts(records), records);

		assertEquals(3, store.findRecords(records.stream().map(StoredTraceRecord::getIdentity)
				.collect(Collectors.toList())).size());

		verify(manager, times(1)).findByDns(eq(TraceRecordEntry.class), any(Collection.class));
	}

	@Test
	void testFindRecords_emptyInput_noOrmCall() {
		assertTrue(store.findRecords(Collections.emptyList()).isEmpty());

		verify(manager, never()).findByDns(any(Class.class), any(Collection.class));
	}

	@Test
	void testFindRecordsByChainPositions_oneOrQueryPerChunk() throws Exception {
		List<StoredTraceRecord> records = records(1, 250);
		store.insertIngestBatch(receipts(records), records);
		List<ChainPosition> positions = records.stream().map(StoredTraceRecord::getChainPosition)
				.collect(Collectors.toList());

		assertEquals(250, store.findRecordsByChainPositions(positions).size());

		ArgumentCaptor<Filter> filters = ArgumentCaptor.forClass(Filter.class);
		verify(manager, times(3)).findEntries(eq("ou=records," + OrmTraceStores.BASE_DN), eq(TraceRecordEntry.class),
				filters.capture());
		List<Integer> chunkSizes = new ArrayList<>();
		for (Filter filter : filters.getAllValues()) {
			assertEquals(FilterType.OR, filter.getType());
			for (Filter keyFilter : filter.getFilters()) {
				assertEquals("jansTraceChainPosKey", keyFilter.getAttributeName());
			}
			chunkSizes.add(filter.getFilters().length);
		}
		assertEquals(List.of(OrmTraceStore.CHAIN_POSITION_LOOKUP_BATCH, OrmTraceStore.CHAIN_POSITION_LOOKUP_BATCH,
				50), chunkSizes);
	}

	@Test
	void testFindRecordsByChainPositions_emptyInput_noOrmCall() {
		assertTrue(store.findRecordsByChainPositions(Collections.emptyList()).isEmpty());

		verify(manager, never()).findEntries(anyString(), any(Class.class), any(Filter.class));
	}

	// -- fixtures --------------------------------------------------------------------------------

	private static List<StoredTraceRecord> records(int firstSeq, int count) {
		List<StoredTraceRecord> records = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			records.add(record(firstSeq + i));
		}
		return records;
	}

	private static StoredTraceRecord record(long seq) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, "producer-1", "record-" + seq);
		ChainIdentity chain = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		String digest = "sha256:" + String.format("%064d", seq);
		String assertion = "{\"trace\":{\"event\":{\"tokens\":[]}},\"producer_chain\":{\"producer_id\":\"producer-1\","
				+ "\"producer_instance_id\":\"instance-1\",\"producer_chain_id\":\"chain-1\",\"sequence_number\":" + seq
				+ "}}";
		return new StoredTraceRecord(identity, assertion, digest, new VerificationResult(true, "kid-1", 1000L, "Ed25519"),
				new ReceiptEntry(seq, 2000L + seq, TraceConstants.ZERO_HASH, digest),
				new IngestionFlags(false, false, false, false), new ExecutionIdentity(DOMAIN, "authority-1", "exec-1"),
				new ChainPosition(chain, seq), TraceConstants.ZERO_HASH, Collections.emptyList(),
				TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 3000L + seq, 0L);
	}

	private static List<TraceReceiptEntry> receipts(List<StoredTraceRecord> records) {
		List<TraceReceiptEntry> receipts = new ArrayList<>(records.size());
		for (StoredTraceRecord record : records) {
			ReceiptEntry receipt = record.getReceipt();
			TraceReceiptEntry row = new TraceReceiptEntry();
			row.setDomainId(DOMAIN);
			row.setReceiptSeq(receipt.getReceiptSequence());
			row.setReceivedAt(new Date(receipt.getReceivedAtMs()));
			row.setReceivedAtMs(receipt.getReceivedAtMs());
			row.setProducerId(record.getIdentity().getProducerId());
			row.setRecordId(record.getIdentity().getRecordId());
			row.setRecordKey(TRACE_KEYS.recordKey(record.getIdentity()));
			row.setContentDigest(record.getContentDigest());
			row.setPrevReceiptHash(receipt.getPrevReceiptHash());
			row.setReceiptHash(receipt.getReceiptHash());
			row.setReceiptStateEnum(TraceReceiptState.COMMITTED);
			row.setNodeId("node-1");
			row.setCreationDate(new Date(receipt.getReceivedAtMs()));
			receipts.add(row);
		}
		return receipts;
	}

}
