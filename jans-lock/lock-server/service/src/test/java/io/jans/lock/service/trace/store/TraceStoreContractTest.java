/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.PolicyRecordQuery;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.model.VerificationResult;

/**
 * Contract that every {@link TraceStore} implementation must satisfy (design §5, §8, §9, §10,
 * §13). {@code InMemoryTraceStore} and the ORM-backed store both run this
 * suite via {@link #createStore()}.
 *
 * <p>JUnit 5, not TestNG: Surefire in {@code lock-server} is pinned to the JUnit-Platform provider
 * and silently skips TestNG classes (confirmed against the pre-existing {@code AuditServiceTest}),
 * so every TRACE test is written in JUnit 5 instead.
 *
 * @author Yuriy Movchan
 */
public abstract class TraceStoreContractTest {

	protected static final String DOMAIN = "default";

	protected static final TraceKeys TRACE_KEYS = TraceTestCrypto.traceKeys();

	protected TraceStore store;

	protected abstract TraceStore createStore();

	@BeforeEach
	void setUpStore() {
		store = createStore();
	}

	// -- fixtures ---------------------------------------------------------------------------

	protected StoredTraceRecord newRecord(String producerId, String recordId, String execAuthority, String execId,
			String chainProducerId, String chainInstanceId, String chainId, long seq, long receiptSeq,
			List<TokenRef> tokenRefs) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, producerId, recordId);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, execAuthority, execId);
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, chainProducerId, chainInstanceId, chainId);
		ChainPosition chainPosition = new ChainPosition(chainIdentity, seq);
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519");
		io.jans.lock.service.trace.model.ReceiptEntry receipt = new io.jans.lock.service.trace.model.ReceiptEntry(
				receiptSeq, 2000L + receiptSeq, TraceConstants.ZERO_HASH, "sha256:" + String.format("%064d", receiptSeq));
		IngestionFlags flags = new IngestionFlags(false, false, false, false);
		return new StoredTraceRecord(identity, "{\"trace\":{}}", "sha256:" + String.format("%064d", receiptSeq),
				verification, receipt, flags, execution, chainPosition, TraceConstants.ZERO_HASH,
				tokenRefs, TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 3000L + receiptSeq, 0L);
	}

	protected StoredTraceRecord newRecord(String producerId, String recordId, String execAuthority, String execId,
			long seq, long receiptSeq) {
		return newRecord(producerId, recordId, execAuthority, execId, producerId, "instance-1", "chain-1", seq,
				receiptSeq, Collections.emptyList());
	}

	protected TraceReceiptEntry newReceiptRow(long seq, long receivedAtMs, String producerId, String recordId,
			TraceReceiptState state) {
		TraceReceiptEntry entity = new TraceReceiptEntry();
		entity.setDomainId(DOMAIN);
		entity.setReceiptSeq(seq);
		entity.setReceivedAt(new Date(receivedAtMs));
		entity.setReceivedAtMs(receivedAtMs);
		entity.setProducerId(producerId);
		entity.setRecordId(recordId);
		entity.setRecordKey(TRACE_KEYS.recordKey(new RecordIdentity(DOMAIN, producerId, recordId)));
		entity.setContentDigest("sha256:" + String.format("%064d", seq));
		entity.setPrevReceiptHash(TraceConstants.ZERO_HASH);
		entity.setReceiptHash("sha256:" + String.format("%064d", seq));
		entity.setReceiptStateEnum(state);
		entity.setNodeId("node-1");
		entity.setCreationDate(new Date(receivedAtMs));
		return entity;
	}

	protected TraceProducerKeyEntry newProducerKey(String producerId, String kid) {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");
		TraceProducerKeyEntry entity = new TraceProducerKeyEntry();
		entity.setDomainId(DOMAIN);
		entity.setProducerId(producerId);
		entity.setKid(kid);
		entity.setPublicKeyJwk(jwk);
		entity.setAuthorizedEventKinds(Arrays.asList("AUTHORIZATION_DECISION", "RUNTIME_EFFECT"));
		entity.setValidFrom(new Date(100L));
		entity.setRegisteredBy("client-1");
		entity.setCreationDate(new Date(100L));
		return entity;
	}

	protected TraceChainEntry newChainRegistration(ChainIdentity chainIdentity, long registeredAtMs,
			String registeredBy) {
		TraceChainEntry entity = new TraceChainEntry();
		entity.setDomainId(chainIdentity.getDomainId());
		entity.setProducerId(chainIdentity.getProducerId());
		entity.setProducerInstanceId(chainIdentity.getProducerInstanceId());
		entity.setProducerChainId(chainIdentity.getProducerChainId());
		entity.setRegisteredBy(registeredBy);
		entity.setCreationDate(new Date(registeredAtMs));
		return entity;
	}

	// -- records: insert / duplicate / find --------------------------------------------------

	@Test
	void testInsertRecord_thenFindRecord_returnsEqualRecord() throws Exception {
		StoredTraceRecord stored = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);

		store.insertRecord(stored);

		Optional<StoredTraceRecord> found = store.findRecord(stored.getIdentity());
		assertTrue(found.isPresent());
		assertEquals(stored.getIdentity(), found.get().getIdentity());
		assertEquals(stored.getContentDigest(), found.get().getContentDigest());
		assertEquals(stored.getAssertionRaw(), found.get().getAssertionRaw());
	}

	@Test
	void testInsertRecord_duplicateIdentity_throwsDuplicateEntryException() throws Exception {
		StoredTraceRecord stored = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(stored);

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
		StoredTraceRecord stored = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(stored);

		StoredTraceRecord found = store.findRecord(stored.getIdentity()).get();
		found.getFlags().setCoverageGap(true);

		StoredTraceRecord foundAgain = store.findRecord(stored.getIdentity()).get();
		assertFalse(foundAgain.getFlags().isCoverageGap());
	}

	// -- records: flags update only changes flags --------------------------------------------

	@Test
	void testUpdateRecordFlags_onlyChangesFlags_preservesLateAndEverythingElse() throws Exception {
		StoredTraceRecord stored = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(stored);

		IngestionFlags newFlags = new IngestionFlags(true, true, true, true);
		boolean updated = store.updateRecordFlags(stored.getIdentity(), newFlags, stored.getVersion());

		assertTrue(updated);
		StoredTraceRecord found = store.findRecord(stored.getIdentity()).get();
		assertTrue(found.getFlags().isCoverageGap());
		assertTrue(found.getFlags().isChainLinkFailure());
		assertTrue(found.getFlags().isEquivocation());
		// jansTraceLate is immutable after ingestion (design §10): the store must not adopt
		// the incoming value even though IngestionFlags carries one.
		assertFalse(found.getFlags().isLate());
		assertEquals(stored.getContentDigest(), found.getContentDigest());
		assertEquals(stored.getAssertionRaw(), found.getAssertionRaw());
	}

	@Test
	void testUpdateRecordFlags_unknownIdentity_returnsFalse() throws Exception {
		boolean updated = store.updateRecordFlags(new RecordIdentity(DOMAIN, "nope", "nope"),
				new IngestionFlags(true, true, true, true), 0L);

		assertFalse(updated);
	}

	// -- records: optimistic-concurrency version ----

	@Test
	void testUpdateRecordFlags_matchingVersion_succeedsAndIncrementsVersion() throws Exception {
		StoredTraceRecord stored = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(stored);
		long initialVersion = store.findRecord(stored.getIdentity()).get().getVersion();

		boolean updated = store.updateRecordFlags(stored.getIdentity(), new IngestionFlags(true, false, false, false),
				initialVersion);

		assertTrue(updated);
		assertEquals(initialVersion + 1, store.findRecord(stored.getIdentity()).get().getVersion());
	}

	@Test
	void testUpdateRecordFlags_staleVersion_throwsVersionConflictReason() throws Exception {
		StoredTraceRecord stored = newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 1L);
		store.insertRecord(stored);
		long initialVersion = store.findRecord(stored.getIdentity()).get().getVersion();
		// Advance the stored version once so the caller's expectation below is stale.
		store.updateRecordFlags(stored.getIdentity(), new IngestionFlags(true, false, false, false), initialVersion);

		RecordIdentity identity = stored.getIdentity();
		IngestionFlags staleFlags = new IngestionFlags(false, true, false, false);
		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> store.updateRecordFlags(identity, staleFlags, initialVersion));

		assertEquals(TraceStore.VERSION_CONFLICT_REASON, ex.getReason());
		// The losing writer's flags must never have been applied.
		assertTrue(store.findRecord(stored.getIdentity()).get().getFlags().isCoverageGap());
		assertFalse(store.findRecord(stored.getIdentity()).get().getFlags().isChainLinkFailure());
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

	// -- records: policy store release search ----------------------------------------------------

	private static StoredTraceRecord withPolicy(StoredTraceRecord r, PolicyRef policy) {
		return new StoredTraceRecord(r.getIdentity(), r.getAssertionRaw(), r.getContentDigest(), r.getVerification(),
				r.getReceipt(), r.getFlags(), r.getExecution(), r.getChainPosition(), r.getPrevRecordHash(),
				r.getTokenRefs(), r.getEventKind(), r.getSignedAt(), r.getNodeId(), r.getCreatedAtMs(), r.getVersion(),
				policy);
	}

	private static List<String> recordIds(List<StoredTraceRecord> records) {
		return records.stream().map(r -> r.getIdentity().getRecordId()).collect(java.util.stream.Collectors.toList());
	}

	@Test
	void testFindRecordsByPolicy_filtersByReleaseAndReceivedRange() throws Exception {
		String hash1 = "sha256:" + "1".repeat(64);
		String hash2 = "sha256:" + "2".repeat(64);
		PolicyRef v1 = new PolicyRef("store-1", "1.0.0", hash1);
		store.insertRecord(withPolicy(newRecord("producer-1", "record-2", "authority-1", "exec-1", 1L, 20L), v1));
		store.insertRecord(withPolicy(newRecord("producer-1", "record-1", "authority-1", "exec-1", 1L, 10L), v1));
		store.insertRecord(withPolicy(newRecord("producer-1", "record-3", "authority-1", "exec-1", 1L, 30L),
				new PolicyRef("store-1", "2.0.0", hash2)));
		store.insertRecord(withPolicy(newRecord("producer-1", "record-4", "authority-1", "exec-1", 1L, 40L),
				new PolicyRef("store-2", "1.0.0", hash1)));
		store.insertRecord(newRecord("producer-1", "record-5", "authority-1", "exec-1", 1L, 50L));

		List<StoredTraceRecord> byRelease = store.findRecordsByPolicy(DOMAIN,
				new PolicyRecordQuery("store-1", "1.0.0", null, null, null), 0, 100);
		assertEquals(Arrays.asList("record-1", "record-2"), recordIds(byRelease));
		assertEquals(v1, byRelease.get(0).getPolicy());

		assertEquals(Arrays.asList("record-1", "record-2", "record-4"), recordIds(
				store.findRecordsByPolicy(DOMAIN, new PolicyRecordQuery(null, null, hash1, null, null), 0, 100)));
		assertEquals(Arrays.asList("record-2", "record-3"), recordIds(store.findRecordsByPolicy(DOMAIN,
				new PolicyRecordQuery("store-1", null, null, 2020L, 2030L), 0, 100)));
		assertEquals(Arrays.asList("record-2"), recordIds(
				store.findRecordsByPolicy(DOMAIN, new PolicyRecordQuery("store-1", null, null, null, null), 1, 1)));
		assertTrue(store.findRecordsByPolicy("other-domain", new PolicyRecordQuery("store-1", null, null, null, null),
				0, 100).isEmpty());
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
				"chain-1", 1L, 1L, Collections.emptyList()));
		store.insertRecord(newRecord("producer-1", "record-b", "authority-1", "exec-b", "producer-1", "instance-1",
				"chain-1", 1L, 2L, Collections.emptyList()));

		List<StoredTraceRecord> found = store.findRecordsByChainPosition(position);

		assertEquals(2, found.size());
	}

	// -- records: token lookups ------------------------------------------------------

	@Test
	void testFindRecordsByToken_jtiReference_matchesRegardlessOfTokenType() throws Exception {
		TokenRef stored = new TokenRef("issuer-1", "access_token", "jti-1", null);
		store.insertRecord(newRecord("producer-1", "record-1", "authority-1", "exec-1", "producer-1", "instance-1",
				"chain-1", 1L, 1L, Arrays.asList(stored)));

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

		TraceReceiptEntry conflicting = newReceiptRow(1L, 2000L, "producer-2", "record-2", TraceReceiptState.PENDING);
		assertThrows(DuplicateEntryException.class, () -> store.insertReceipt(conflicting));
	}

	@Test
	void testSettleReceipt_thenFindReceipt_reflectsNewState() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));

		ReceiptSettlement settled = store.settleReceipt(DOMAIN, 1L, TraceReceiptState.COMMITTED);

		assertEquals(ReceiptSettlement.SETTLED, settled);
		Optional<TraceReceiptEntry> found = store.findReceipt(DOMAIN, 1L);
		assertTrue(found.isPresent());
		assertEquals(TraceReceiptState.COMMITTED, found.get().getReceiptStateEnum());
	}

	@Test
	void testFindReceipts_returnsExistingSequencesOfTheDomainOnly() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.COMMITTED));
		store.insertReceipt(newReceiptRow(2L, 2000L, "producer-1", "record-2", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(3L, 3000L, "producer-1", "record-3", TraceReceiptState.COMMITTED));

		List<TraceReceiptEntry> found = store.findReceipts(DOMAIN, List.of(1L, 3L, 99L, 1L));

		assertEquals(Set.of(1L, 3L), found.stream().map(TraceReceiptEntry::getReceiptSeq).collect(Collectors.toSet()));
		assertTrue(store.findReceipts("other-domain", List.of(1L)).isEmpty());
	}

	private static TracePolicyReleaseEntry policyRelease(String hash, long servedAtMs) {
		TracePolicyReleaseEntry entry = new TracePolicyReleaseEntry();
		entry.setPolicyStoreId("store-1");
		entry.setBundleHash(hash);
		entry.setPolicyStoreVersion("1.0.0");
		entry.setFirstServedAtMs(servedAtMs);
		entry.setLastServedAtMs(servedAtMs);
		return entry;
	}

	@Test
	void testPolicyRelease_insertFindUpdate() throws Exception {
		String hash = "sha256:" + "a".repeat(64);
		store.insertPolicyRelease(policyRelease(hash, 1000L));

		assertTrue(store.recordPolicyReleaseServed("store-1", hash, "1.0.1", 5000L));
		TracePolicyReleaseEntry found = store.findPolicyRelease("store-1", hash).orElseThrow();

		assertEquals(1000L, found.getFirstServedAtMs());
		assertEquals(5000L, found.getLastServedAtMs());
		assertEquals("1.0.1", found.getPolicyStoreVersion());
		assertFalse(store.findPolicyRelease("store-1", "sha256:" + "b".repeat(64)).isPresent());
		assertFalse(store.recordPolicyReleaseServed("store-2", hash, "1.0.0", 5000L));
	}

	@Test
	void testPolicyRelease_servedTimesOnlyWiden() throws Exception {
		String hash = "sha256:" + "d".repeat(64);
		store.insertPolicyRelease(policyRelease(hash, 5000L));

		assertTrue(store.recordPolicyReleaseServed("store-1", hash, "0.9.0", 3000L));
		assertTrue(store.recordPolicyReleaseServed("store-1", hash, "1.0.0", 4000L));
		TracePolicyReleaseEntry found = store.findPolicyRelease("store-1", hash).orElseThrow();

		assertEquals(3000L, found.getFirstServedAtMs());
		assertEquals(5000L, found.getLastServedAtMs());
		assertEquals("1.0.0", found.getPolicyStoreVersion());
	}

	@Test
	void testPolicyRelease_findOldAndRemove() throws Exception {
		String oldHash = "sha256:" + "1".repeat(64);
		String recentHash = "sha256:" + "2".repeat(64);
		store.insertPolicyRelease(policyRelease(oldHash, 1000L));
		store.insertPolicyRelease(policyRelease(recentHash, 9000L));

		List<TracePolicyReleaseEntry> old = store.findPolicyReleasesLastServedBefore(5000L, 10);

		assertEquals(List.of(oldHash), old.stream().map(TracePolicyReleaseEntry::getBundleHash).collect(Collectors.toList()));
		assertTrue(store.removePolicyReleaseLastServedBefore("store-1", oldHash, 5000L));
		assertFalse(store.findPolicyRelease("store-1", oldHash).isPresent());
		assertFalse(store.removePolicyReleaseLastServedBefore("store-1", oldHash, 5000L));
		assertFalse(store.removePolicyReleaseLastServedBefore("store-1", recentHash, 5000L));
		assertTrue(store.findPolicyRelease("store-1", recentHash).isPresent());
		assertTrue(store.findPolicyReleasesLastServedBefore(5000L, 0).isEmpty());
	}

	@Test
	void testPolicyRelease_secondInsertIsDuplicate() throws Exception {
		String hash = "sha256:" + "c".repeat(64);
		store.insertPolicyRelease(policyRelease(hash, 1000L));
		TracePolicyReleaseEntry again = policyRelease(hash, 2000L);

		assertThrows(DuplicateEntryException.class, () -> store.insertPolicyRelease(again));
		assertEquals(1000L, store.findPolicyRelease("store-1", hash).orElseThrow().getLastServedAtMs());
	}

	@Test
	void testSettleReceipt_unknownSequence_notFound() throws Exception {
		assertEquals(ReceiptSettlement.NOT_FOUND, store.settleReceipt(DOMAIN, 999L, TraceReceiptState.VOID));
	}

	@Test
	void testSettleReceipt_pendingTarget_rejected() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));

		assertThrows(IllegalArgumentException.class, () -> store.settleReceipt(DOMAIN, 1L, TraceReceiptState.PENDING));
		assertEquals(TraceReceiptState.PENDING, store.findReceipt(DOMAIN, 1L).get().getReceiptStateEnum());
	}

	@Test
	void testSettleReceipt_sameTerminalStateTwice_alreadySettled() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));

		assertEquals(ReceiptSettlement.SETTLED, store.settleReceipt(DOMAIN, 1L, TraceReceiptState.VOID));
		assertEquals(ReceiptSettlement.ALREADY_SETTLED, store.settleReceipt(DOMAIN, 1L, TraceReceiptState.VOID));
		assertEquals(TraceReceiptState.VOID, store.findReceipt(DOMAIN, 1L).get().getReceiptStateEnum());
	}

	@Test
	void testSettleReceipt_otherTerminalState_conflictAndUnchanged() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1000L, "producer-1", "record-1", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(2L, 2000L, "producer-1", "record-2", TraceReceiptState.PENDING));

		assertEquals(ReceiptSettlement.SETTLED, store.settleReceipt(DOMAIN, 1L, TraceReceiptState.COMMITTED));
		assertEquals(ReceiptSettlement.CONFLICT, store.settleReceipt(DOMAIN, 1L, TraceReceiptState.VOID));
		assertEquals(TraceReceiptState.COMMITTED, store.findReceipt(DOMAIN, 1L).get().getReceiptStateEnum());

		assertEquals(ReceiptSettlement.SETTLED, store.settleReceipt(DOMAIN, 2L, TraceReceiptState.VOID));
		assertEquals(ReceiptSettlement.CONFLICT, store.settleReceipt(DOMAIN, 2L, TraceReceiptState.COMMITTED));
		assertEquals(TraceReceiptState.VOID, store.findReceipt(DOMAIN, 2L).get().getReceiptStateEnum());
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

		List<TraceReceiptEntry> pending = store.findPendingReceiptsOlderThan(2000L, 100);

		assertEquals(1, pending.size());
		assertEquals(1L, pending.get(0).getReceiptSeq());
	}

	@Test
	void testFindPendingReceiptsOlderThan_receivedExactlyAtCutoff_excluded() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 1999L, "producer-1", "record-1", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(2L, 2000L, "producer-1", "record-2", TraceReceiptState.PENDING));

		List<TraceReceiptEntry> pending = store.findPendingReceiptsOlderThan(2000L, 100);

		assertEquals(1, pending.size());
		assertEquals(1L, pending.get(0).getReceiptSeq());
	}

	@Test
	void testFindPendingReceiptsOlderThan_respectsLimit() throws Exception {
		for (long seq = 1; seq <= 5; seq++) {
			store.insertReceipt(newReceiptRow(seq, 1000L, "producer-1", "record-" + seq, TraceReceiptState.PENDING));
		}

		List<TraceReceiptEntry> pending = store.findPendingReceiptsOlderThan(5000L, 2);

		assertEquals(2, pending.size());
	}

	@Test
	void testFindPendingReceiptsOlderThan_oldestFirstBeforeLimit() throws Exception {
		store.insertReceipt(newReceiptRow(1L, 3000L, "producer-1", "record-1", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(2L, 1000L, "producer-1", "record-2", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(3L, 4000L, "producer-1", "record-3", TraceReceiptState.PENDING));
		store.insertReceipt(newReceiptRow(4L, 2000L, "producer-1", "record-4", TraceReceiptState.PENDING));

		List<TraceReceiptEntry> pending = store.findPendingReceiptsOlderThan(5000L, 2);

		assertEquals(2, pending.size());
		assertEquals(2L, pending.get(0).getReceiptSeq());
		assertEquals(4L, pending.get(1).getReceiptSeq());
	}

	// -- registries: chains CRUD ------------------------------------------------------------------

	@Test
	void testInsertChain_thenFindChain_found() throws Exception {
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		store.insertChain(newChainRegistration(chainIdentity, 1000L, "client-1"));

		Optional<TraceChainEntry> found = store.findChain(chainIdentity);

		assertTrue(found.isPresent());
		assertEquals("client-1", found.get().getRegisteredBy());
	}

	@Test
	void testInsertChain_duplicate_throwsDuplicateEntryException() throws Exception {
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		store.insertChain(newChainRegistration(chainIdentity, 1000L, "client-1"));

		TraceChainEntry conflicting = newChainRegistration(chainIdentity, 2000L, "client-2");
		assertThrows(DuplicateEntryException.class, () -> store.insertChain(conflicting));
	}

	@Test
	void testFindChains_filtersByProducerWhenGiven() throws Exception {
		store.insertChain(newChainRegistration(new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1"), 1000L,
				"client-1"));
		store.insertChain(newChainRegistration(new ChainIdentity(DOMAIN, "producer-2", "instance-1", "chain-2"), 1000L,
				"client-1"));

		assertEquals(2, store.findChains(DOMAIN, null).size());
		assertEquals(1, store.findChains(DOMAIN, "producer-1").size());
	}

	// -- registries: producer keys CRUD and revoke -------------------------------------------------

	@Test
	void testInsertProducerKey_thenFindProducerKey_found() throws Exception {
		store.insertProducerKey(newProducerKey("producer-1", "kid-1"));

		Optional<TraceProducerKeyEntry> found = store.findProducerKey(DOMAIN, "producer-1", "kid-1");

		assertTrue(found.isPresent());
		assertEquals("kid-1", found.get().getKid());
		assertEquals(Arrays.asList("AUTHORIZATION_DECISION", "RUNTIME_EFFECT"), found.get().getAuthorizedEventKinds());
		assertNull(found.get().getRevokedAtMs());
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
		Optional<TraceProducerKeyEntry> found = store.findProducerKey(DOMAIN, "producer-1", "kid-1");
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

	// -- ingest batch: atomic receipts + records ----------------------------------------------

	/** A store whose {@code isAtomicIngestSupported()} is {@code false}. */
	protected abstract TraceStore createNonAtomicStore();

	/** The COMMITTED receipt row the batch path stores for {@code record}, matching its embedded receipt. */
	protected TraceReceiptEntry batchReceiptFor(StoredTraceRecord record) {
		io.jans.lock.service.trace.model.ReceiptEntry receipt = record.getReceipt();
		TraceReceiptEntry row = newReceiptRow(receipt.getReceiptSequence(), receipt.getReceivedAtMs(),
				record.getIdentity().getProducerId(), record.getIdentity().getRecordId(), TraceReceiptState.COMMITTED);
		row.setDomainId(record.getIdentity().getDomainId());
		row.setPrevReceiptHash(receipt.getPrevReceiptHash());
		row.setReceiptHash(receipt.getReceiptHash());
		row.setContentDigest(record.getContentDigest());
		return row;
	}

	/** The record's embedded receipt fields equal the receipt row stored in the same batch. */
	protected static void assertReceiptMatchesRecord(TraceReceiptEntry row, StoredTraceRecord record) {
		io.jans.lock.service.trace.model.ReceiptEntry receipt = record.getReceipt();
		assertEquals(record.getIdentity().getDomainId(), row.getDomainId());
		assertEquals(receipt.getReceiptSequence(), row.getReceiptSeq().longValue());
		assertEquals(receipt.getReceivedAtMs(), row.getReceivedAtMs().longValue());
		assertEquals(receipt.getPrevReceiptHash(), row.getPrevReceiptHash());
		assertEquals(receipt.getReceiptHash(), row.getReceiptHash());
		assertEquals(record.getIdentity().getProducerId(), row.getProducerId());
		assertEquals(record.getIdentity().getRecordId(), row.getRecordId());
		assertEquals(TRACE_KEYS.recordKey(record.getIdentity()), row.getRecordKey());
		assertEquals(record.getContentDigest(), row.getContentDigest());
	}

	private List<StoredTraceRecord> batchRecords(int firstReceiptSeq, int count) {
		List<StoredTraceRecord> records = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			long seq = firstReceiptSeq + i;
			records.add(newRecord("producer-1", "record-" + seq, "authority-1", "exec-1", seq, seq));
		}
		return records;
	}

	private List<TraceReceiptEntry> batchReceipts(List<StoredTraceRecord> records) {
		return records.stream().map(this::batchReceiptFor).collect(Collectors.toList());
	}

	private void assertNothingStored(List<StoredTraceRecord> records) {
		for (StoredTraceRecord record : records) {
			assertFalse(store.findRecord(record.getIdentity()).isPresent(), "record " + record.getIdentity());
			assertFalse(store.findReceipt(DOMAIN, record.getReceipt().getReceiptSequence()).isPresent(),
					"receipt " + record.getReceipt().getReceiptSequence());
		}
	}

	@Test
	void testIsAtomicIngestSupported_true() {
		assertTrue(store.isAtomicIngestSupported());
	}

	@Test
	void testInsertIngestBatch_storesAllReceiptsAndRecords() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 3);
		List<TraceReceiptEntry> receipts = batchReceipts(records);

		store.insertIngestBatch(receipts, records);

		for (StoredTraceRecord record : records) {
			StoredTraceRecord found = store.findRecord(record.getIdentity()).get();
			assertEquals(record.getContentDigest(), found.getContentDigest());
			assertEquals(record.getChainPosition(), found.getChainPosition());
			TraceReceiptEntry row = store.findReceipt(DOMAIN, record.getReceipt().getReceiptSequence()).get();
			assertEquals(TraceReceiptState.COMMITTED, row.getReceiptStateEnum());
			assertEquals("node-1", row.getNodeId());
			assertReceiptMatchesRecord(row, found);
		}
		assertEquals(3L, store.findReceiptHead(DOMAIN).get().getReceiptSequence());
		assertEquals(3L, store.countRecords(DOMAIN));
	}

	@Test
	void testInsertIngestBatch_keepsCallerReceiptState() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 1);
		List<TraceReceiptEntry> receipts = batchReceipts(records);
		receipts.get(0).setReceiptStateEnum(TraceReceiptState.PENDING);

		store.insertIngestBatch(receipts, records);

		assertEquals(TraceReceiptState.PENDING, store.findReceipt(DOMAIN, 1L).get().getReceiptStateEnum());
	}

	@Test
	void testInsertIngestBatch_emptyBatch_noOp() throws Exception {
		store.insertIngestBatch(Collections.emptyList(), Collections.emptyList());

		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
	}

	@Test
	void testInsertIngestBatch_existingReceiptPosition_throwsAndStoresNothing() throws Exception {
		store.insertReceipt(newReceiptRow(2L, 1000L, "producer-9", "record-other", TraceReceiptState.PENDING));
		List<StoredTraceRecord> records = batchRecords(1, 3);

		assertThrows(DuplicateEntryException.class, () -> store.insertIngestBatch(batchReceipts(records), records));

		for (StoredTraceRecord record : records) {
			assertFalse(store.findRecord(record.getIdentity()).isPresent());
		}
		assertFalse(store.findReceipt(DOMAIN, 1L).isPresent());
		assertFalse(store.findReceipt(DOMAIN, 3L).isPresent());
		assertEquals("record-other", store.findReceipt(DOMAIN, 2L).get().getRecordId());
	}

	@Test
	void testInsertIngestBatch_existingRecordIdentity_throwsAndStoresNoReceipts() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 3);
		StoredTraceRecord existing = newRecord("producer-1", "record-3", "authority-1", "exec-1", 3L, 99L);
		store.insertRecord(existing);

		assertThrows(DuplicateEntryException.class, () -> store.insertIngestBatch(batchReceipts(records), records));

		for (long seq = 1; seq <= 3; seq++) {
			assertFalse(store.findReceipt(DOMAIN, seq).isPresent(), "receipt " + seq);
		}
		assertFalse(store.findRecord(records.get(0).getIdentity()).isPresent());
		assertFalse(store.findRecord(records.get(1).getIdentity()).isPresent());
		assertEquals(99L, store.findRecord(existing.getIdentity()).get().getReceipt().getReceiptSequence());
	}

	@Test
	void testInsertIngestBatch_samePositionTwiceInBatch_throwsAndStoresNothing() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 2);
		List<TraceReceiptEntry> receipts = batchReceipts(records);
		receipts.add(batchReceiptFor(records.get(0)));

		assertThrows(DuplicateEntryException.class, () -> store.insertIngestBatch(receipts, records));

		assertNothingStored(records);
	}

	@Test
	void testInsertIngestBatch_sameIdentityTwiceInBatch_throwsAndStoresNothing() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 2);
		List<TraceReceiptEntry> receipts = batchReceipts(records);
		List<StoredTraceRecord> withRepeat = new java.util.ArrayList<>(records);
		withRepeat.add(records.get(1));

		assertThrows(DuplicateEntryException.class, () -> store.insertIngestBatch(receipts, withRepeat));

		assertNothingStored(records);
	}

	@Test
	void testInsertIngestBatch_nonAtomicStore_throwsUnsupportedOperation() {
		TraceStore nonAtomic = createNonAtomicStore();
		List<StoredTraceRecord> records = batchRecords(1, 1);

		assertFalse(nonAtomic.isAtomicIngestSupported());
		assertThrows(UnsupportedOperationException.class,
				() -> nonAtomic.insertIngestBatch(batchReceipts(records), records));
		assertFalse(nonAtomic.findRecord(records.get(0).getIdentity()).isPresent());
	}

	// -- multi-get: records by identity -----------------------------------------------------------

	@Test
	void testFindRecords_foundAndMissingMix() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 2);
		store.insertIngestBatch(batchReceipts(records), records);
		RecordIdentity missing = new RecordIdentity(DOMAIN, "producer-1", "missing");

		Map<RecordIdentity, StoredTraceRecord> found = store.findRecords(
				Arrays.asList(records.get(1).getIdentity(), missing, records.get(0).getIdentity()));

		assertEquals(Arrays.asList(records.get(1).getIdentity(), records.get(0).getIdentity()),
				new java.util.ArrayList<>(found.keySet()));
		assertEquals(records.get(0).getContentDigest(), found.get(records.get(0).getIdentity()).getContentDigest());
		assertFalse(found.containsKey(missing));
	}

	@Test
	void testFindRecords_emptyInput_emptyMap() {
		assertTrue(store.findRecords(Collections.emptyList()).isEmpty());
	}

	@Test
	void testFindRecords_moreIdentitiesThanOneChunk() throws Exception {
		List<RecordIdentity> identities = new java.util.ArrayList<>();
		for (int from = 1; from <= 300; from += 100) {
			List<StoredTraceRecord> records = batchRecords(from, 100);
			store.insertIngestBatch(batchReceipts(records), records);
			records.forEach(r -> identities.add(r.getIdentity()));
		}
		for (int i = 0; i < 250; i++) {
			identities.add(new RecordIdentity(DOMAIN, "producer-1", "missing-" + i));
		}

		Map<RecordIdentity, StoredTraceRecord> found = store.findRecords(identities);

		assertEquals(300, found.size());
		assertEquals(identities.subList(0, 300), new java.util.ArrayList<>(found.keySet()));
	}

	// -- multi-get: records by chain position -----------------------------------------------------

	@Test
	void testFindRecordsByChainPositions_severalPositionsEquivocationAndMissing() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 3);
		StoredTraceRecord equivocating = newRecord("producer-1", "record-equivocating", "authority-1", "exec-1", 2L, 4L);
		List<StoredTraceRecord> all = new java.util.ArrayList<>(records);
		all.add(equivocating);
		store.insertIngestBatch(batchReceipts(all), all);
		ChainPosition empty = new ChainPosition(records.get(0).getChainPosition().getChainIdentity(), 50L);

		Map<ChainPosition, List<StoredTraceRecord>> found = store.findRecordsByChainPositions(Arrays.asList(
				records.get(0).getChainPosition(), records.get(1).getChainPosition(), empty,
				records.get(2).getChainPosition()));

		assertEquals(3, found.size());
		assertFalse(found.containsKey(empty));
		assertEquals(Arrays.asList("record-1"), recordIdsOf(found.get(records.get(0).getChainPosition())));
		assertEquals(Arrays.asList("record-2", "record-equivocating"),
				recordIdsOf(found.get(records.get(1).getChainPosition())));
		assertEquals(Arrays.asList("record-3"), recordIdsOf(found.get(records.get(2).getChainPosition())));
	}

	@Test
	void testFindRecordsByChainPositions_emptyInput_emptyMap() {
		assertTrue(store.findRecordsByChainPositions(Collections.emptyList()).isEmpty());
	}

	@Test
	void testFindRecordsByChainPositions_morePositionsThanOneChunk() throws Exception {
		List<StoredTraceRecord> records = batchRecords(1, 250);
		store.insertIngestBatch(batchReceipts(records), records);
		List<ChainPosition> positions = records.stream().map(StoredTraceRecord::getChainPosition)
				.collect(Collectors.toList());
		positions.add(new ChainPosition(positions.get(0).getChainIdentity(), 1000L));

		Map<ChainPosition, List<StoredTraceRecord>> found = store.findRecordsByChainPositions(positions);

		assertEquals(250, found.size());
		for (StoredTraceRecord record : records) {
			assertEquals(Arrays.asList(record.getIdentity().getRecordId()),
					recordIdsOf(found.get(record.getChainPosition())));
		}
	}

	private static List<String> recordIdsOf(List<StoredTraceRecord> records) {
		return records.stream().map(r -> r.getIdentity().getRecordId()).collect(Collectors.toList());
	}

}
