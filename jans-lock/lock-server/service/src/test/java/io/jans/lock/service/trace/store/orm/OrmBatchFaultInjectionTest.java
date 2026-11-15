/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.service.trace.error.TraceErrors;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.ingest.BatchElement;
import io.jans.lock.service.trace.ingest.BatchOutcome;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ReceiptBatchItem;
import io.jans.lock.service.trace.receipt.ReceiptRun;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.orm.PersistenceEntryManager;

/**
 * Fault injection on the batch path over {@link OrmTraceStore} and a live SQL database (task 08,
 * deliverable 3). Opt-in through {@value DatabaseProfile#PROPERTY}; skipped without it. Three
 * faults, each checked for: nothing of the failed attempt is stored, the chain has no gap, the next
 * batch continues from the right head, and the outcomes and the stored state equal what the single
 * path gives for the same elements (a reference harness ingests them one by one into a second
 * throw-away domain).
 * <ol>
 * <li>The range action throws after the head was read ({@link #actionThrowsAfterHeadRead}), and the
 * same failure raised by the store under the ingest service ({@link #storeFailureFallsBackToSinglePath}).</li>
 * <li>{@code persistBatch} fails mid-flight inside the database: the last receipt row of the batch
 * gets a {@code jansTraceNodeId} longer than its {@code varchar(64)} column, so PostgreSQL rejects
 * the transaction after the earlier rows were already written to it and rolls it back
 * ({@link #databaseRejectsLastRowMidFlight}). Closing the pooled connection is not reachable
 * through the ORM API, and a statement timeout would need a server setting; the oversized value
 * exercises the same rollback path at the database.</li>
 * <li>The last record of the batch duplicates an identity another node stored between the batch's
 * identity pre-read and its head read ({@link #lastRecordDuplicatesIdentityStoredByOtherNode}).</li>
 * </ol>
 */
@EnabledIfSystemProperty(named = DatabaseProfile.PROPERTY, matches = ".+")
class OrmBatchFaultInjectionTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String PRODUCER = "fault-producer";

	private static final String KID = PRODUCER + "-kid";

	private static final String INSTANCE = "fault-instance";

	private static final String CHAIN = "fault-chain";

	private static PersistenceEntryManager entryManager;

	private String domain;

	private String referenceDomain;

	private TraceTestHarness node;

	private TraceTestHarness reference;

	private KeyPair key;

	private List<String> chain;

	@BeforeAll
	static void connect() throws Exception {
		entryManager = DatabaseProfile.connect();
	}

	@AfterAll
	static void disconnect() {
		if (entryManager != null) {
			entryManager.destroy();
		}
	}

	@BeforeEach
	void setUp() {
		domain = DatabaseProfile.newDomain("fault");
		referenceDomain = DatabaseProfile.newDomain("fault-ref");
		node = newHarness("fault-node-a");
		reference = newHarness("fault-reference");
		key = node.registerKey(domain, PRODUCER, KID);
		node.registerChain(domain, PRODUCER, INSTANCE, CHAIN);
		reference.registerKey(referenceDomain, PRODUCER, KID, key);
		reference.registerChain(referenceDomain, PRODUCER, INSTANCE, CHAIN);
		chain = TraceRecordBuilder.linkedChain(key, PRODUCER, KID, INSTANCE, CHAIN, 8);
	}

	@AfterEach
	void removeDomains() {
		DatabaseProfile.removeDomain(entryManager, domain);
		DatabaseProfile.removeDomain(entryManager, referenceDomain);
	}

	private static TraceTestHarness newHarness(String nodeId) {
		return new TraceTestHarness(TraceTestCrypto.javaProvider(),
				keys -> new InstrumentedTraceStore(OrmTraceStores.create(entryManager, keys))).setNodeId(nodeId);
	}

	private static InstrumentedTraceStore store(TraceTestHarness harness) {
		return (InstrumentedTraceStore) harness.traceStore();
	}

	// -- (a) the action throws after the head read ------------------------------------------------

	@Test
	void actionThrowsAfterHeadRead() {
		ingestSingle(node, domain, chain.subList(0, 2));
		long headBefore = head(node, domain);
		assertEquals(2, headBefore);

		List<ReceiptBatchItem> items = Arrays.asList(
				new ReceiptBatchItem(PRODUCER, "r-3", "k-3", "sha256:" + "3".repeat(64), node.now()),
				new ReceiptBatchItem(PRODUCER, "r-4", "k-4", "sha256:" + "4".repeat(64), node.now()));
		AtomicReference<ReceiptRun> seen = new AtomicReference<>();
		TraceStorageException thrown = assertThrows(TraceStorageException.class,
				() -> node.receiptChain().runWithRange(domain, items, node.nodeId(), run -> {
					seen.set(run);
					throw new TraceStorageException("injected", "action failure after the head read");
				}));
		assertEquals("injected", thrown.getReason());
		assertEquals(3, seen.get().getPositions().get(0).getSeq(), "the action saw the positions after the head");
		assertEquals(0, seen.get().getAttempt(), "a non-duplicate failure is not retried");

		assertEquals(headBefore, head(node, domain), "nothing of the failed run is stored");
		assertEquals(2, DatabaseProfile.countReceipts(entryManager, domain));

		List<Object> next = ingestBatch(node, domain, chain.subList(2, 5));
		for (Object outcome : next) {
			assertTrue(outcome instanceof AcceptanceResult, String.valueOf(outcome));
		}
		assertEquals(3, ((AcceptanceResult) next.get(0)).getReceiptSequence(), "the next batch continues from the head");
		assertEquals(5, head(node, domain));
		assertAudit(5);
	}

	@Test
	void storeFailureFallsBackToSinglePath() {
		ingestSingle(node, domain, chain.subList(0, 2));
		ingestSingle(reference, referenceDomain, chain.subList(0, 2));
		store(node).resetCounters();
		List<String> elements = chain.subList(2, 6);

		store(node).failNextIngestBatch(() -> new TraceStorageException("persist_failed", "injected store failure"));
		List<Object> actual = ingestBatch(node, domain, elements);
		List<Object> expected = ingestSingleOutcomes(reference, referenceDomain, elements);

		assertEquals(1, store(node).batchFailures());
		assertEquals(4, store(node).recordInserts(), "the fallback wrote the elements one by one");
		assertSameOutcomes(expected, actual);
		assertEquals(3, ((AcceptanceResult) actual.get(0)).getReceiptSequence());
		assertSameRecords(chain.subList(0, 6));
		assertAudit(6);
	}

	// -- (b) the database rejects the transaction mid-flight -----------------------------------------

	@Test
	void databaseRejectsLastRowMidFlight() {
		ingestSingle(node, domain, chain.subList(0, 2));
		ingestSingle(reference, referenceDomain, chain.subList(0, 2));
		store(node).resetCounters();
		List<String> elements = chain.subList(2, 7);

		String tooLong = "n".repeat(65);
		List<String> batchRecordDns = new ArrayList<>();
		store(node).editNextIngestBatch((receipts, records) -> {
			receipts.get(receipts.size() - 1).setNodeId(tooLong);
			for (StoredTraceRecord record : records) {
				batchRecordDns.add(recordDn(record.getIdentity()));
			}
		});
		AtomicReference<String> observed = new AtomicReference<>();
		store(node).observeIngestBatchFailures(failure -> {
			// Seen before the ingest service falls back: the rolled-back rows must be absent.
			Optional<ReceiptHead> head = OrmTraceStores.create(entryManager).findReceiptHead(domain);
			int receipts = DatabaseProfile.countReceipts(entryManager, domain);
			int records = entryManager.findByDns(TraceRecordEntry.class, batchRecordDns).size();
			observed.set("head=" + head.map(ReceiptHead::getReceiptSequence).orElse(0L) + " receipts=" + receipts
					+ " batchRecords=" + records);
		});

		List<Object> actual = ingestBatch(node, domain, elements);
		List<Object> expected = ingestSingleOutcomes(reference, referenceDomain, elements);

		assertEquals("head=2 receipts=2 batchRecords=0", observed.get(), "state right after the rollback");
		assertEquals(1, store(node).batchFailures());
		assertEquals(0, store(node).batchDuplicates());
		Exception failure = store(node).lastBatchFailure();
		assertTrue(failure instanceof TraceStorageException, String.valueOf(failure));
		assertEquals("persist_failed", ((TraceStorageException) failure).getReason());
		assertTrue(messages(failure).contains("character varying(64)"), "the database rejected the oversized row: " + messages(failure));
		assertEquals(5, store(node).recordInserts(), "the fallback wrote the elements one by one");

		assertSameOutcomes(expected, actual);
		assertEquals(3, ((AcceptanceResult) actual.get(0)).getReceiptSequence());
		assertSameRecords(chain.subList(0, 7));
		assertAudit(7);
	}

	// -- (c) the last record duplicates an identity written by another node -------------------------

	@Test
	void lastRecordDuplicatesIdentityStoredByOtherNode() {
		ingestSingle(node, domain, chain.subList(0, 2));
		ingestSingle(reference, referenceDomain, chain.subList(0, 2));
		TraceTestHarness otherNode = newHarness("fault-node-b");
		store(node).resetCounters();
		List<String> elements = chain.subList(2, 5);
		String last = elements.get(2);

		// Between the batch's identity pre-read and its head read, node B stores the last element.
		store(node).beforeNextHeadRead(() -> ingestSingle(otherNode, domain, List.of(last)));

		List<Object> actual = ingestBatch(node, domain, elements);

		assertEquals(1, store(node).batchDuplicates(), "the first attempt lost the identity race");
		assertEquals(2, store(node).batchCalls(), "the re-planned batch was committed on the second attempt");
		assertEquals(0, store(node).recordInserts(), "no fallback");

		ingestSingle(reference, referenceDomain, List.of(last));
		List<Object> expected = ingestSingleOutcomes(reference, referenceDomain, elements);
		assertSameOutcomes(expected, actual);
		AcceptanceResult replay = (AcceptanceResult) actual.get(2);
		AcceptanceResult referenceReplay = (AcceptanceResult) expected.get(2);
		assertTrue(replay.isIdempotentReplay());
		assertTrue(referenceReplay.isIdempotentReplay());
		// The replay is classified from the row after the batch's commit and post-commit correlation,
		// whose re-read of position 4's successor cleared the coverage gap node B stored at position 5.
		assertFalse(replay.isCoverageGapFlag(), "replay outcome carries the flags after the commit");
		assertFalse(referenceReplay.isCoverageGapFlag());
		assertEquals(3, ((AcceptanceResult) actual.get(2)).getReceiptSequence(), "node B took position 3");
		assertEquals(4, ((AcceptanceResult) actual.get(0)).getReceiptSequence());
		assertEquals(5, ((AcceptanceResult) actual.get(1)).getReceiptSequence());
		assertSameRecords(chain.subList(0, 5));
		StoredTraceRecord successor = node.traceStore()
				.findRecord(new RecordIdentity(domain, PRODUCER, recordId(last))).orElseThrow();
		assertFalse(successor.getFlags().isCoverageGap(), "position 5 is corrected after the commit");
		assertAudit(5);
	}

	// -- helpers ------------------------------------------------------------------------------------

	private static long head(TraceTestHarness harness, String domainId) {
		return harness.traceStore().findReceiptHead(domainId).map(ReceiptHead::getReceiptSequence).orElse(0L);
	}

	private static String recordDn(RecordIdentity identity) {
		TraceKeys keys = new TraceKeys(TraceTestCrypto.javaProvider());
		return TraceKeys.recordDn(OrmTraceStores.BASE_DN, keys.recordKey(identity));
	}

	private static void ingestSingle(TraceTestHarness harness, String domainId, List<String> jsons) {
		for (String json : jsons) {
			harness.ingest(json, harness.ctx(domainId));
		}
	}

	private static List<Object> ingestSingleOutcomes(TraceTestHarness harness, String domainId, List<String> jsons) {
		List<Object> outcomes = new ArrayList<>();
		for (String json : jsons) {
			try {
				outcomes.add(harness.ingest(json, harness.ctx(domainId)));
			} catch (RuntimeException ex) {
				outcomes.add(ex);
			}
		}
		return outcomes;
	}

	private static List<Object> ingestBatch(TraceTestHarness harness, String domainId, List<String> jsons) {
		List<BatchElement> elements = new ArrayList<>();
		for (int i = 0; i < jsons.size(); i++) {
			elements.add(new BatchElement(i, jsons.get(i).getBytes(StandardCharsets.UTF_8)));
		}
		List<Object> outcomes = new ArrayList<>();
		for (BatchOutcome outcome : harness.ingestionService().ingestBatch(elements, harness.ctx(domainId))) {
			outcomes.add(outcome.isAccepted() ? outcome.getResult() : outcome.getError());
		}
		harness.drainExecutors();
		return outcomes;
	}

	private static String describe(Object outcome) {
		if (outcome instanceof RuntimeException) {
			TraceErrors.Classification c = TraceErrors.classify((RuntimeException) outcome);
			return "ERROR[" + c.getType() + ", " + c.getReason() + "]";
		}
		return String.valueOf(outcome);
	}

	private static void assertSameOutcomes(List<Object> expected, List<Object> actual) {
		assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(describe(expected.get(i)), describe(actual.get(i)), "outcome of element " + i);
		}
	}

	/** Every record of {@code jsons} is stored the same way in both domains: receipt, flags, position, node-independent fields. */
	private void assertSameRecords(List<String> jsons) {
		for (String json : jsons) {
			String recordId = recordId(json);
			StoredTraceRecord a = node.traceStore().findRecord(new RecordIdentity(domain, PRODUCER, recordId)).orElse(null);
			StoredTraceRecord e = reference.traceStore().findRecord(new RecordIdentity(referenceDomain, PRODUCER, recordId))
					.orElse(null);
			assertNotNull(a, "record " + recordId + " in the batch domain");
			assertNotNull(e, "record " + recordId + " in the reference domain");
			assertEquals(e.getContentDigest(), a.getContentDigest(), recordId);
			// The receipt hash binds the domain: the two domains differ, so sequence and time are compared.
			assertEquals(e.getReceipt().getReceiptSequence(), a.getReceipt().getReceiptSequence(), recordId + " receipt");
			assertEquals(e.getReceipt().getReceivedAtMs(), a.getReceipt().getReceivedAtMs(), recordId + " receipt");
			assertEquals(e.getFlags(), a.getFlags(), recordId + " flags");
			assertEquals(e.getChainPosition().getSequenceNumber(), a.getChainPosition().getSequenceNumber(), recordId);
			assertEquals(e.getPrevRecordHash(), a.getPrevRecordHash(), recordId);
			TraceReceiptEntry receipt = node.traceStore().findReceipt(domain, a.getReceipt().getReceiptSequence()).orElse(null);
			assertNotNull(receipt, "receipt of " + recordId);
			assertEquals(recordId, receipt.getRecordId());
			assertEquals(a.getContentDigest(), receipt.getContentDigest());
		}
	}

	private void assertAudit(long expectedRecords) {
		DomainAudit audit = new DomainAudit(node.traceStore(), entryManager, domain).run(expectedRecords, true,
				node.receiptChain());
		assertTrue(audit.ok(), audit::toString);
		assertFalse(audit.head() != expectedRecords, audit::toString);
	}

	private static String recordId(String json) {
		try {
			return MAPPER.readTree(json).path("record_id").asText();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String messages(Throwable failure) {
		StringBuilder out = new StringBuilder();
		for (Throwable t = failure; t != null; t = t.getCause()) {
			out.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" | ");
		}
		return out.toString();
	}

}
