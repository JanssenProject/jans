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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ChainRegistration;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.ReceiptRow;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;

/**
 * Contract that every {@link TraceStore} implementation must satisfy (design §5, §8, §9, §10,
 * §13). {@code InMemoryTraceStore} (this task) and the ORM-backed store (task 13) both run this
 * suite via {@link #createStore()}.
 *
 * <p>JUnit 5, not TestNG: Surefire in {@code lock-server} is pinned to the JUnit-Platform provider
 * and silently skips TestNG classes (confirmed against the pre-existing {@code AuditServiceTest}),
 * so every TRACE test in this series (tasks 02 onward) is written in JUnit 5 instead.
 *
 * @author Yuriy Movchan
 */
public abstract class TraceStoreContractTest {

	protected static final String DOMAIN = "default";

	protected TraceStore store;

	protected abstract TraceStore createStore();

	@BeforeEach
	void setUpStore() {
		store = createStore();
	}

	// -- fixtures ---------------------------------------------------------------------------

	protected StoredTraceRecord newRecord(String producerId, String recordId, String execAuthority, String execId,
			String chainProducerId, String chainInstanceId, String chainId, long seq, long receiptSeq,
			List<String> capabilityIds, List<TokenRef> tokenRefs) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, producerId, recordId);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, execAuthority, execId);
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, chainProducerId, chainInstanceId, chainId);
		ChainPosition chainPosition = new ChainPosition(chainIdentity, seq);
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519");
		io.jans.lock.service.trace.model.ReceiptEntry receipt = new io.jans.lock.service.trace.model.ReceiptEntry(
				receiptSeq, 2000L + receiptSeq, TraceConstants.ZERO_HASH, "sha256:" + String.format("%064d", receiptSeq));
		IngestionFlags flags = new IngestionFlags(false, false, false, false);
		return new StoredTraceRecord(identity, "{\"trace\":{}}", "sha256:" + String.format("%064d", receiptSeq),
				verification, receipt, flags, execution, chainPosition, TraceConstants.ZERO_HASH, capabilityIds,
				tokenRefs, TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 3000L + receiptSeq);
	}

	protected StoredTraceRecord newRecord(String producerId, String recordId, String execAuthority, String execId,
			long seq, long receiptSeq) {
		return newRecord(producerId, recordId, execAuthority, execId, producerId, "instance-1", "chain-1", seq,
				receiptSeq, Collections.emptyList(), Collections.emptyList());
	}

	protected ReceiptRow newReceiptRow(long seq, long receivedAtMs, String producerId, String recordId,
			TraceReceiptState state) {
		return new ReceiptRow(DOMAIN, seq, receivedAtMs, producerId, recordId,
				TraceKeys.recordKey(new RecordIdentity(DOMAIN, producerId, recordId)),
				"sha256:" + String.format("%064d", seq), TraceConstants.ZERO_HASH,
				"sha256:" + String.format("%064d", seq), state, "node-1");
	}

	protected ProducerKey newProducerKey(String producerId, String kid) {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");
		return new ProducerKey(DOMAIN, producerId, kid, jwk, 100L, null, null, "client-1");
	}

	// -- records: insert / duplicate / find --------------------------------------------------

	@Test
	void testInsertRecord_thenFindRecord_returnsEqualRecord() throws Exception {
		StoredTraceRecord record = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);

		store.insertRecord(record);

		Optional<StoredTraceRecord> found = store.findRecord(record.getIdentity());
		assertTrue(found.isPresent());
		assertEquals(record.getIdentity(), found.get().getIdentity());
		assertEquals(record.getContentDigest(), found.get().getContentDigest());
		assertEquals(record.getAssertionRaw(), found.get().getAssertionRaw());
	}

	@Test
	void testInsertRecord_duplicateIdentity_throwsDuplicateEntryException() throws Exception {
		StoredTraceRecord record = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(record);

		StoredTraceRecord conflicting = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 2L);

		assertThrows(DuplicateEntryException.class, () -> store.insertRecord(conflicting));
	}

	@Test
	void testFindRecord_unknownIdentity_empty() {
		Optional<StoredTraceRecord> found = store.findRecord(new RecordIdentity(DOMAIN, "nope", "nope"));

		assertFalse(found.isPresent());
	}

	@Test
	void testFindRecord_returnsDeepCopy_mutatingFlagsDoesNotAffectStore() throws Exception {
		StoredTraceRecord record = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(record);

		StoredTraceRecord found = store.findRecord(record.getIdentity()).get();
		found.getFlags().setCoverageGap(true);

		StoredTraceRecord foundAgain = store.findRecord(record.getIdentity()).get();
		assertFalse(foundAgain.getFlags().isCoverageGap());
	}

	// -- records: flags update only changes flags --------------------------------------------

	@Test
	void testUpdateRecordFlags_onlyChangesFlags_preservesLateAndEverythingElse() throws Exception {
		StoredTraceRecord record = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(record);

		IngestionFlags newFlags = new IngestionFlags(true, true, true, true);
		boolean updated = store.updateRecordFlags(record.getIdentity(), newFlags);

		assertTrue(updated);
		StoredTraceRecord found = store.findRecord(record.getIdentity()).get();
		assertTrue(found.getFlags().isCoverageGap());
		assertTrue(found.getFlags().isChainLinkFailure());
		assertTrue(found.getFlags().isEquivocation());
		// jansTraceLate is immutable after ingestion (design §10, T-1): the store must not adopt
		// the incoming value even though IngestionFlags carries one.
		assertFalse(found.getFlags().isLate());
		assertEquals(record.getContentDigest(), found.getContentDigest());
		assertEquals(record.getAssertionRaw(), found.getAssertionRaw());
	}

	@Test
	void testUpdateRecordFlags_unknownIdentity_returnsFalse() throws Exception {
		boolean updated = store.updateRecordFlags(new RecordIdentity(DOMAIN, "nope", "nope"),
				new IngestionFlags(true, true, true, true));

		assertFalse(updated);
	}

	// -- records: execution ordering -----------------------------------------------------------

	@Test
	void testFindRecordsByExecution_orderedByReceiptSequenceAscending() throws Exception {
		StoredTraceRecord third = newRecord("producer-1", "record-3", "authority-1", "exec-1", 1L, 30L);
		StoredTraceRecord first = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 10L);
		StoredTraceRecord second = newRecord("producer-1", "record-2", "authority-1", "exec-1", 1L, 20L);
		store.insertRecord(third);
		store.insertRecord(first);
		store.insertRecord(second);

		List<StoredTraceRecord> found = store.findRecordsByExecution(new ExecutionIdentity(DOMAIN, "authority-1", "exec-1"),
				0, 100);

		assertEquals(3, found.size());
		assertEquals("record-1", found.get(0).getIdentity().getRecordId());
		assertEquals("record-2", found.get(1).getIdentity().getRecordId());
		assertEquals("record-3", found.get(2).getIdentity().getRecordId());
	}

	@Test
	void testFindRecordsByExecution_paging() throws Exception {
		for (int i = 1; i <= 5; i++) {
			store.insertRecord(newRecord("producer-1", "record-" + i, "authority-1", "exec-1", 1L, i));
		}

		List<StoredTraceRecord> page = store.findRecordsByExecution(new ExecutionIdentity(DOMAIN, "authority-1", "exec-1"),
				2, 2);

		assertEquals(2, page.size());
		assertEquals("record-3", page.get(0).getIdentity().getRecordId());
		assertEquals("record-4", page.get(1).getIdentity().getRecordId());
	}

	// -- records: bare-id ambiguity data --------------------------------------------------------

	@Test
	void testFindRecordsByBareRecordId_multipleProducers_returnsAll() throws Exception {
		store.insertRecord(newRecord("producer-1", "shared-record", "authority-1", "exec-1", 1L, 1L));
		store.insertRecord(newRecord("producer-2", "shared-record", "authority-1", "exec-2", 1L, 2L));

		List<StoredTraceRecord> found = store.findRecordsByBareRecordId(DOMAIN, "shared-record");

		assertEquals(2, found.size());
	}

	@Test
	void testFindExecutionAuthorities_multipleAuthorities_distinct() throws Exception {
		store.insertRecord(newRecord("producer-1", "record-1", "authority-1", "shared-exec", 1L, 1L));
		store.insertRecord(newRecord("producer-2", "record-2", "authority-2", "shared-exec", 1L, 2L));
		store.insertRecord(newRecord("producer-3", "record-3", "authority-1", "shared-exec", 1L, 3L));

		List<String> authorities = store.findExecutionAuthorities(DOMAIN, "shared-exec");

		assertEquals(2, authorities.size());
		assertTrue(authorities.contains("authority-1"));
		assertTrue(authorities.contains("authority-2"));
	}

	// -- records: chain position multi-row (equivocation) ---------------------------------------

	@Test
	void testFindRecordsByChainPosition_multipleRecords_isEquivocationNotError() throws Exception {
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		ChainPosition position = new ChainPosition(chainIdentity, 1L);
		store.insertRecord(newRecord("producer-1", "record-a", "authority-1", "exec-a", "producer-1", "instance-1",
				"chain-1", 1L, 1L, Collections.emptyList(), Collections.emptyList()));
		store.insertRecord(newRecord("producer-1", "record-b", "authority-1", "exec-b", "producer-1", "instance-1",
				"chain-1", 1L, 2L, Collections.emptyList(), Collections.emptyList()));

		List<StoredTraceRecord> found = store.findRecordsByChainPosition(position);

		assertEquals(2, found.size());
	}

	// -- records: capability / token lookups ------------------------------------------------------

	@Test
	void testFindRecordsByCapability_matchesByDerivedKeyNotRawEquality() throws Exception {
		store.insertRecord(newRecord("producer-1", "record-1", "authority-1", "exec-1", "producer-1", "instance-1",
				"chain-1", 1L, 1L, Arrays.asList("cap-1", "cap-2"), Collections.emptyList()));
		store.insertRecord(newRecord("producer-1", "record-2", "authority-1", "exec-2", "producer-1", "instance-1",
				"chain-1", 2L, 2L, Arrays.asList("cap-2"), Collections.emptyList()));

		List<StoredTraceRecord> found = store.findRecordsByCapability(DOMAIN, "cap-2");

		assertEquals(2, found.size());
	}

	@Test
	void testFindRecordsByToken_jtiReference_matchesRegardlessOfTokenType() throws Exception {
		TokenRef stored = new TokenRef("issuer-1", "access_token", "jti-1", null);
		store.insertRecord(newRecord("producer-1", "record-1", "authority-1", "exec-1", "producer-1", "instance-1",
				"chain-1", 1L, 1L, Collections.emptyList(), Arrays.asList(stored)));

		TokenRef search = new TokenRef("issuer-1", "refresh_token", "jti-1", null);
		List<StoredTraceRecord> found = store.findRecordsByToken(DOMAIN, search);

		assertEquals(1, found.size());
	}

	@Test
	void testCountRecords_scopedByDomain() throws Exception {
		store.insertRecord(newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L));
		store.insertRecord(newRecord("producer-2", "record-2", "authority-1", "exec-2", 1L, 2L));

		assertEquals(2, store.countRecords(DOMAIN));
		assertEquals(0, store.countRecords("other-domain"));
	}

	// -- receipts: insert / duplicate ------------------------------------------------------------

	@Test
	void testInsertReceipt_duplicateSequence_throwsDuplicateEntryException() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));

		ReceiptRow conflicting = newReceiptRow(1L, 2000L, "producer-2", "record-2", TraceReceiptState.PENDING);
		assertThrows(DuplicateEntryException.class, () -> store.insertReceipt(conflicting));
	}

	@Test
	void testUpdateReceiptState_thenFindReceipt_reflectsNewState() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));

		boolean updated = store.updateReceiptState(DOMAIN, 1L, TraceReceiptState.COMMITTED);

		assertTrue(updated);
		Optional<ReceiptRow> found = store.findReceipt(DOMAIN, 1L);
		assertTrue(found.isPresent());
		assertEquals(TraceReceiptState.COMMITTED, found.get().getState());
	}

	@Test
	void testUpdateReceiptState_unknownSequence_returnsFalse() throws Exception {
		boolean updated = store.updateReceiptState(DOMAIN, 999L, TraceReceiptState.VOID);

		assertFalse(updated);
	}

	// -- receipts: head across states --------------------------------------------------------------

	@Test
	void testFindReceiptHead_consideredAcrossAllStates() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.COMMITTED));
		store.insertReceipt(newReceiptRow(2L, 2000L, "producer-1", "record-2", TraceReceiptState.VOID));
		store.insertReceipt(newReceiptRow(3L, 3000L, "producer-1", "record-3", TraceReceiptState.PENDING));

		Optional<ReceiptHead> head = store.findReceiptHead(DOMAIN);

		assertTrue(head.isPresent());
		assertEquals(3L, head.get().getReceiptSequence());
	}

	@Test
	void testFindReceiptHead_emptyDomain_empty() {
		assertFalse(store.findReceiptHead("empty-domain").isPresent());
	}

	// -- receipts: pending cutoff ---------------------------------------------------------------

	@Test
	void testFindPendingReceiptsOlderThan_onlyPendingBeforeCutoff() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(2L, 5000L, "producer-1", "record-2", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(3L, 1000L, "producer-1", "record-3", TraceReceiptState.COMMITTED));

		List<ReceiptRow> pending = store.findPendingReceiptsOlderThan(2000L, 100);

		assertEquals(1, pending.size());
		assertEquals(1L, pending.get(0).getReceiptSequence());
	}

	@Test
	void testFindPendingReceiptsOlderThan_respectsLimit() throws Exception {
		for (long seq = 1; seq <= 5; seq++) {
			store.insertReceipt(newReceiptRow(seq, 1000L, "producer-1", "record-" + seq, TraceReceiptState.PENDING));
		}

		List<ReceiptRow> pending = store.findPendingReceiptsOlderThan(5000L, 2);

		assertEquals(2, pending.size());
	}

	// -- registries: chains CRUD ------------------------------------------------------------------

	@Test
	void testInsertChain_thenFindChain_found() throws Exception {
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		store.insertChain(new ChainRegistration(chainIdentity, 1000L, "client-1"));

		Optional<ChainRegistration> found = store.findChain(chainIdentity);

		assertTrue(found.isPresent());
		assertEquals("client-1", found.get().getRegisteredBy());
	}

	@Test
	void testInsertChain_duplicate_throwsDuplicateEntryException() throws Exception {
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		store.insertChain(new ChainRegistration(chainIdentity, 1000L, "client-1"));

		ChainRegistration conflicting = new ChainRegistration(chainIdentity, 2000L, "client-2");
		assertThrows(DuplicateEntryException.class, () -> store.insertChain(conflicting));
	}

	@Test
	void testFindChains_filtersByProducerWhenGiven() throws Exception {
		store.insertChain(new ChainRegistration(new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1"), 1000L,
				"client-1"));
		store.insertChain(new ChainRegistration(new ChainIdentity(DOMAIN, "producer-2", "instance-1", "chain-2"), 1000L,
				"client-1"));

		assertEquals(2, store.findChains(DOMAIN, null).size());
		assertEquals(1, store.findChains(DOMAIN, "producer-1").size());
	}

	// -- registries: producer keys CRUD and revoke -------------------------------------------------

	@Test
	void testInsertProducerKey_thenFindProducerKey_found() throws Exception {
		store.insertProducerKey(newProducerKey("producer-1", "kid-1"));

		Optional<ProducerKey> found = store.findProducerKey(DOMAIN, "producer-1", "kid-1");

		assertTrue(found.isPresent());
		assertEquals("kid-1", found.get().getKid());
		assertFalse(found.get().getRevokedAtMs() != null);
	}

	@Test
	void testInsertProducerKey_duplicate_throwsDuplicateEntryException() throws Exception {
		store.insertProducerKey(newProducerKey("producer-1", "kid-1"));

		assertThrows(DuplicateEntryException.class, () -> store.insertProducerKey(newProducerKey("producer-1", "kid-1")));
	}

	@Test
	void testRevokeProducerKey_setsRevokedAt_idempotent() throws Exception {
		store.insertProducerKey(newProducerKey("producer-1", "kid-1"));

		boolean first = store.revokeProducerKey(DOMAIN, "producer-1", "kid-1", 5000L);
		boolean second = store.revokeProducerKey(DOMAIN, "producer-1", "kid-1", 9000L);

		assertTrue(first);
		assertTrue(second);
		Optional<ProducerKey> found = store.findProducerKey(DOMAIN, "producer-1", "kid-1");
		assertTrue(found.isPresent());
		assertEquals(Long.valueOf(5000L), found.get().getRevokedAtMs());
	}

	@Test
	void testRevokeProducerKey_unknownKey_returnsFalse() throws Exception {
		boolean revoked = store.revokeProducerKey(DOMAIN, "nope", "nope", 1000L);

		assertFalse(revoked);
	}

	@Test
	void testFindProducerKeys_filtersByProducerWhenGiven() throws Exception {
		store.insertProducerKey(newProducerKey("producer-1", "kid-1"));
		store.insertProducerKey(newProducerKey("producer-1", "kid-2"));
		store.insertProducerKey(newProducerKey("producer-2", "kid-1"));

		assertEquals(3, store.findProducerKeys(DOMAIN, null).size());
		assertEquals(2, store.findProducerKeys(DOMAIN, "producer-1").size());
	}

}
