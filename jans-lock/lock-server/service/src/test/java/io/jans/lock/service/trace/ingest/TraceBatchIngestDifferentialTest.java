/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceErrors;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.receipt.ChainVerificationReport;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;

/**
 * The gate of the batch pipeline: for any list of elements {@code ingestBatch} gives the same
 * per-element outcomes and leaves the same stored state as calling {@code ingest} per element in
 * order on an identical starting store. The only difference allowed is the receipt state during
 * the run; both end {@code COMMITTED}.
 */
public class TraceBatchIngestDifferentialTest {

	private static final String DOMAIN = "domain-a";

	/** The evidence domain each harness writes; both are {@link #DOMAIN} unless a subclass separates them. */
	private String sequentialDomain;

	private String batchDomain;

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN_1 = "chain-01JABC9Z0K";

	private static final String CHAIN_2 = "chain-03JABC9Z0K";

	private static final String CHAIN_UNREGISTERED = "chain-99JABC9Z0K";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TraceTestHarness sequential;

	private TraceTestHarness batch;

	private KeyPair key;

	@BeforeEach
	void setUp() {
		sequential = newHarness();
		batch = newHarness();
		sequentialDomain = sequentialDomain();
		batchDomain = batchDomain();
		key = sequential.registerKey(sequentialDomain, PRODUCER, KID);
		batch.registerKey(batchDomain, PRODUCER, KID, key);
		for (String chain : new String[] { CHAIN_1, CHAIN_2 }) {
			sequential.registerChain(sequentialDomain, PRODUCER, INSTANCE, chain);
			batch.registerChain(batchDomain, PRODUCER, INSTANCE, chain);
		}
	}

	/** Override to run the same scenarios over another {@code TraceStore}. */
	protected TraceTestHarness newHarness() {
		return new TraceTestHarness();
	}

	/**
	 * The domain the one-by-one harness writes. Two harnesses over separate stores use the same
	 * domain; a subclass whose harnesses share one database returns two different throw-away domains.
	 */
	protected String sequentialDomain() {
		return DOMAIN;
	}

	/** The domain the batch harness writes; see {@link #sequentialDomain()}. */
	protected String batchDomain() {
		return DOMAIN;
	}

	private String domainOf(TraceTestHarness harness) {
		return harness == sequential ? sequentialDomain : batchDomain;
	}

	// -- helpers ------------------------------------------------------------------------------

	private static String digestOf(String json) {
		try {
			return CanonicalHashes.sha256PrefixedOfJcs(MAPPER.readTree(json));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String record(String chain, long seq, String prevDigest) {
		return TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE).producerChainId(chain)
				.sequenceNumber(seq).prevRecordHash(prevDigest).sign(key.getPrivate());
	}

	/** A properly linked chain of {@code count} records starting at position 1. */
	private List<String> chain(String chain, int count) {
		List<String> records = new ArrayList<>();
		String prev = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= count; seq++) {
			String json = record(chain, seq, prev);
			records.add(json);
			prev = digestOf(json);
		}
		return records;
	}

	private String resignedWithOtherContent(String json) {
		return TraceRecordBuilder.fromJson(json).withField("9.9.9-conflict", "trace", "policy", "policy_store_version")
				.sign(key.getPrivate());
	}

	private String tampered(String json) {
		return TraceRecordBuilder.tamperAfterSigning(json, "9.9.9-tampered", "trace", "policy", "policy_store_version");
	}

	/** Outcomes of the one-by-one path. */
	private List<Object> runSequential(List<String> elements) {
		List<Object> outcomes = new ArrayList<>();
		for (String json : elements) {
			try {
				outcomes.add(sequential.ingest(json, sequential.ctx(sequentialDomain)));
			} catch (RuntimeException ex) {
				outcomes.add(ex);
			}
		}
		return outcomes;
	}

	private List<Object> runBatch(List<String> elements) {
		List<BatchElement> list = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++) {
			list.add(new BatchElement(i, elements.get(i).getBytes(StandardCharsets.UTF_8)));
		}
		List<Object> outcomes = new ArrayList<>();
		for (BatchOutcome outcome : batch.ingestionService().ingestBatch(list, batch.ctx(batchDomain))) {
			outcomes.add(outcome.isAccepted() ? outcome.getResult() : outcome.getError());
		}
		return outcomes;
	}

	private static String describe(Object outcome) {
		if (outcome instanceof RuntimeException) {
			TraceErrors.Classification c = TraceErrors.classify((RuntimeException) outcome);
			return "ERROR[" + c.getType() + ", " + c.getReason() + "]";
		}
		return String.valueOf(outcome);
	}

	/**
	 * Runs {@code preload} through the single path on both stores, then {@code elements} each way, and compares.
	 *
	 * @return the outcomes of the batch path, equal to those of the one-by-one path
	 */
	private List<Object> assertSameResult(List<String> preload, List<String> elements) {
		remember(preload);
		remember(elements);
		for (String json : preload) {
			sequential.ingest(json, sequential.ctx(sequentialDomain));
			batch.ingest(json, batch.ctx(batchDomain));
		}

		List<Object> expected = runSequential(elements);
		List<Object> actual = runBatch(elements);
		batch.drainExecutors();

		assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(describe(expected.get(i)), describe(actual.get(i)), "outcome of element " + i);
		}
		long fresh = expected.stream().filter(o -> o instanceof AcceptanceResult
				&& !((AcceptanceResult) o).isIdempotentReplay()).count();
		if (fresh > 0 && ingestBatchCalls() >= 0) {
			assertTrue(ingestBatchCalls() >= 1, "the batch path must have written the new records");
		}
		assertSameStore();
		return actual;
	}

	/** The stored {@code chain_link_failure} of {@code json}'s record, after checking both stores agree. */
	private boolean storedChainLinkFailure(String json) {
		String recordId;
		try {
			recordId = MAPPER.readTree(json).path("record_id").asText();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		boolean expected = sequential.traceStore().findRecord(new RecordIdentity(sequentialDomain, PRODUCER, recordId))
				.orElseThrow(AssertionError::new).getFlags().isChainLinkFailure();
		boolean actual = batch.traceStore().findRecord(new RecordIdentity(batchDomain, PRODUCER, recordId))
				.orElseThrow(AssertionError::new).getFlags().isChainLinkFailure();
		assertEquals(expected, actual, "both paths store the same chain_link_failure");
		return actual;
	}

	private static AcceptanceResult replayOf(Object outcome) {
		assertTrue(outcome instanceof AcceptanceResult, () -> "expected a replay, got " + describe(outcome));
		AcceptanceResult result = (AcceptanceResult) outcome;
		assertTrue(result.isIdempotentReplay());
		return result;
	}

	/** Record ids of every element, so the stores can be read through the {@code TraceStore} API alone. */
	private final List<String> knownRecordIds = new ArrayList<>();

	private void remember(List<String> jsons) {
		for (String json : jsons) {
			try {
				String recordId = MAPPER.readTree(json).path("record_id").asText(null);
				if (recordId != null && !knownRecordIds.contains(recordId)) {
					knownRecordIds.add(recordId);
				}
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	private List<StoredTraceRecord> storedRecords(TraceTestHarness harness, List<String> recordIds) {
		List<StoredTraceRecord> records = new ArrayList<>();
		for (String recordId : recordIds) {
			harness.traceStore().findRecord(new RecordIdentity(domainOf(harness), PRODUCER, recordId)).ifPresent(records::add);
		}
		records.sort((x, y) -> Long.compare(x.getReceipt().getReceiptSequence(), y.getReceipt().getReceiptSequence()));
		return records;
	}

	private List<TraceReceiptEntry> storedReceipts(TraceTestHarness harness) {
		List<TraceReceiptEntry> receipts = new ArrayList<>();
		String domain = domainOf(harness);
		long head = harness.traceStore().findReceiptHead(domain).map(h -> h.getReceiptSequence()).orElse(0L);
		for (long seq = 1; seq <= head; seq++) {
			harness.traceStore().findReceipt(domain, seq).ifPresent(receipts::add);
		}
		return receipts;
	}

	/** Number of {@code insertIngestBatch} calls, or -1 when the store does not count them. */
	private int ingestBatchCalls() {
		return batch.traceStore() instanceof InMemoryTraceStore ? ((InMemoryTraceStore) batch.traceStore()).getIngestBatches()
				: -1;
	}

	/** The two harnesses may write different domains: compare the position without it. */
	private static ChainPosition withoutDomain(ChainPosition position) {
		ChainIdentity id = position.getChainIdentity();
		return new ChainPosition(new ChainIdentity("", id.getProducerId(), id.getProducerInstanceId(), id.getProducerChainId()),
				position.getSequenceNumber());
	}

	private static ExecutionIdentity withoutDomain(ExecutionIdentity execution) {
		return execution == null ? null
				: new ExecutionIdentity("", execution.getExecutionAuthority(), execution.getTraceExecutionId());
	}

	private void assertSameStore() {
		List<StoredTraceRecord> expected = storedRecords(sequential, knownRecordIds);
		List<StoredTraceRecord> actual = storedRecords(batch, knownRecordIds);
		assertEquals(expected.size(), actual.size(), "stored record count");
		for (int i = 0; i < expected.size(); i++) {
			StoredTraceRecord e = expected.get(i);
			StoredTraceRecord a = actual.get(i);
			String label = "record " + e.getIdentity();
			assertEquals(e.getIdentity().getProducerId(), a.getIdentity().getProducerId(), label);
			assertEquals(e.getIdentity().getRecordId(), a.getIdentity().getRecordId(), label);
			assertEquals(e.getAssertionRaw(), a.getAssertionRaw(), label);
			assertEquals(e.getContentDigest(), a.getContentDigest(), label);
			assertSameReceipt(e.getReceipt(), a.getReceipt(), label + " receipt");
			assertEquals(e.getFlags(), a.getFlags(), label + " flags");
			assertEquals(withoutDomain(e.getChainPosition()), withoutDomain(a.getChainPosition()), label);
			assertEquals(e.getPrevRecordHash(), a.getPrevRecordHash(), label);
			assertEquals(withoutDomain(e.getExecution()), withoutDomain(a.getExecution()), label);
			assertEquals(e.getNodeId(), a.getNodeId(), label);
			assertEquals(e.getCreatedAtMs(), a.getCreatedAtMs(), label);
			assertEquals(e.getVersion(), a.getVersion(), label + " version");
		}

		List<TraceReceiptEntry> expectedReceipts = storedReceipts(sequential);
		List<TraceReceiptEntry> actualReceipts = storedReceipts(batch);
		assertEquals(expectedReceipts.size(), actualReceipts.size(), "receipt count");
		for (int i = 0; i < expectedReceipts.size(); i++) {
			TraceReceiptEntry e = expectedReceipts.get(i);
			TraceReceiptEntry a = actualReceipts.get(i);
			String label = "receipt " + e.getReceiptSeq();
			assertEquals(e.getReceiptSeq(), a.getReceiptSeq(), label);
			if (sameDomain()) {
				assertEquals(e.getReceiptHash(), a.getReceiptHash(), label);
				assertEquals(e.getPrevReceiptHash(), a.getPrevReceiptHash(), label);
				assertEquals(e.getRecordKey(), a.getRecordKey(), label);
			}
			assertEquals(e.getContentDigest(), a.getContentDigest(), label);
			assertEquals(e.getReceivedAtMs(), a.getReceivedAtMs(), label);
			assertEquals(e.getNodeId(), a.getNodeId(), label);
			assertEquals(e.getReceiptStateEnum(), a.getReceiptStateEnum(), label + " state");
		}

		ChainVerificationReport report = batch.verifyDomain(batchDomain);
		assertTrue(report.isOk(), () -> "batch store chain: " + report);
		assertEquals(report.getHeadSequence(), report.getEntries().size(), "receipt chain must be dense");
		if (!sameDomain()) {
			ChainVerificationReport sequentialReport = sequential.verifyDomain(sequentialDomain);
			assertTrue(sequentialReport.isOk(), () -> "sequential store chain: " + sequentialReport);
		}
	}

	private boolean sameDomain() {
		return sequentialDomain.equals(batchDomain);
	}

	/**
	 * The receipt hash and the record key bind the domain id, so two domains give different values
	 * for the same records: they are compared only when both harnesses write one domain, and each
	 * domain's chain is verified on its own instead.
	 */
	private void assertSameReceipt(ReceiptEntry e, ReceiptEntry a, String label) {
		if (sameDomain()) {
			assertEquals(e, a, label);
			return;
		}
		assertEquals(e.getReceiptSequence(), a.getReceiptSequence(), label);
		assertEquals(e.getReceivedAtMs(), a.getReceivedAtMs(), label);
	}

	// -- scenarios ----------------------------------------------------------------------------

	@Test
	void cleanChainOfOneHundred() {
		assertSameResult(Collections.emptyList(), chain(CHAIN_1, 100));
		assertEquals(100, storedRecords(batch, knownRecordIds).size());
		if (ingestBatchCalls() >= 0) {
			assertEquals(1, ingestBatchCalls(), "one atomic write for the whole chain");
		}
	}

	@Test
	void missingSequenceIsCoverageGap() {
		List<String> c = chain(CHAIN_1, 6);
		assertSameResult(Collections.emptyList(), List.of(c.get(0), c.get(1), c.get(3), c.get(4), c.get(5)));
	}

	@Test
	void wrongPrevRecordHashIsChainLinkFailure() {
		List<String> c = chain(CHAIN_1, 3);
		String broken = record(CHAIN_1, 3, TraceConstants.HASH_PREFIX + "1".repeat(64));
		assertSameResult(Collections.emptyList(), List.of(c.get(0), c.get(1), broken));
	}

	@Test
	void twoRecordsAtOnePositionInsideBatchAreEquivocation() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		assertSameResult(Collections.emptyList(), List.of(c.get(0), c.get(1), fork, c.get(2)));
	}

	@Test
	void equivocationAgainstRecordStoredBeforeBatch() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		assertSameResult(List.of(c.get(0), c.get(1)), List.of(fork, c.get(2)));
	}

	@Test
	void forkNextToMatchingPredecessorLeavesStoredSuccessorLinked() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		List<Object> outcomes = assertSameResult(List.of(c.get(0), c.get(1), c.get(2)), List.of(fork, c.get(2)));
		assertFalse(storedChainLinkFailure(c.get(2)), "the stored predecessor still matches");
		assertFalse(replayOf(outcomes.get(1)).isChainLinkFailureFlag());
	}

	@Test
	void matchingPredecessorBeforeForkLeavesStoredSuccessorLinked() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		List<Object> outcomes = assertSameResult(List.of(c.get(0), c.get(2)), List.of(c.get(1), fork, c.get(2)));
		assertFalse(storedChainLinkFailure(c.get(2)));
		assertFalse(replayOf(outcomes.get(2)).isChainLinkFailureFlag());
	}

	@Test
	void forkBeforeMatchingPredecessorBreaksStoredSuccessor() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		List<Object> outcomes = assertSameResult(List.of(c.get(0), c.get(2)), List.of(fork, c.get(1), c.get(2)));
		assertTrue(storedChainLinkFailure(c.get(2)), "only the fork was there when the gap closed; the flag never clears");
		assertTrue(replayOf(outcomes.get(2)).isChainLinkFailureFlag());
	}

	@Test
	void successorEarlierInBatchThenForkThenMatchingPredecessor() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		List<Object> outcomes = assertSameResult(List.of(c.get(0)), List.of(c.get(2), fork, c.get(1), c.get(2)));
		assertTrue(storedChainLinkFailure(c.get(2)));
		assertTrue(replayOf(outcomes.get(3)).isChainLinkFailureFlag());
	}

	@Test
	void successorEarlierInBatchThenMatchingPredecessorThenFork() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		List<Object> outcomes = assertSameResult(List.of(c.get(0)), List.of(c.get(2), c.get(1), fork, c.get(2)));
		assertFalse(storedChainLinkFailure(c.get(2)));
		assertFalse(replayOf(outcomes.get(3)).isChainLinkFailureFlag());
	}

	@Test
	void successorAlreadyStoredOutOfOrderArrival() {
		List<String> c = chain(CHAIN_1, 4);
		assertSameResult(List.of(c.get(2), c.get(3)), List.of(c.get(0), c.get(1)));
	}

	@Test
	void successorStoredWithBrokenLinkGetsChainLinkFailure() {
		List<String> c = chain(CHAIN_1, 3);
		String brokenSuccessor = record(CHAIN_1, 3, TraceConstants.HASH_PREFIX + "2".repeat(64));
		assertSameResult(List.of(brokenSuccessor), List.of(c.get(0), c.get(1)));
	}

	@Test
	void successorEarlierInBatchIsUpdatedWhenPredecessorArrives() {
		List<String> c = chain(CHAIN_1, 4);
		assertSameResult(Collections.emptyList(), List.of(c.get(3), c.get(2), c.get(1), c.get(0)));
	}

	@Test
	void replayOfEarlierRecord() {
		List<String> c = chain(CHAIN_1, 4);
		assertSameResult(List.of(c.get(0), c.get(1), c.get(2)), List.of(c.get(1), c.get(3)));
	}

	@Test
	void replayOfStoredRecordSeesFlagsChangedByEarlierBatchElement() {
		List<String> c = chain(CHAIN_1, 3);
		String fork = record(CHAIN_1, 2, digestOf(c.get(0)));
		assertSameResult(List.of(c.get(0), c.get(1)), List.of(fork, c.get(1)));
	}

	@Test
	void sameRecordTwiceInBatch() {
		List<String> c = chain(CHAIN_1, 3);
		assertSameResult(Collections.emptyList(), List.of(c.get(0), c.get(1), c.get(0), c.get(0)));
	}

	@Test
	void inBatchReplaySeesFlagsAsTheyWereAtThatPoint() {
		List<String> c = chain(CHAIN_1, 3);
		assertSameResult(Collections.emptyList(), List.of(c.get(2), c.get(0), c.get(2), c.get(1), c.get(2)));
	}

	@Test
	void sameIdentityWithTwoDigests() {
		List<String> c = chain(CHAIN_1, 2);
		assertSameResult(Collections.emptyList(),
				List.of(c.get(0), resignedWithOtherContent(c.get(0)), c.get(1), resignedWithOtherContent(c.get(0))));
	}

	@Test
	void invalidSignatureInTheMiddle() {
		List<String> c = chain(CHAIN_1, 4);
		assertSameResult(Collections.emptyList(), List.of(c.get(0), c.get(1), tampered(c.get(2)), c.get(3)));
	}

	@Test
	void unregisteredChainInTheMiddle() {
		List<String> c = chain(CHAIN_1, 3);
		String stray = record(CHAIN_UNREGISTERED, 1, TraceConstants.ZERO_HASH);
		assertSameResult(Collections.emptyList(), List.of(c.get(0), stray, c.get(1), stray, c.get(2)));
	}

	@Test
	void lateRecord() {
		List<String> c = chain(CHAIN_1, 2);
		String late = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE).producerChainId(CHAIN_1)
				.sequenceNumber(3).prevRecordHash(digestOf(c.get(1)))
				.signedAt(TraceTestHarness.DEFAULT_NOW_MS / 1000 - 10_000).sign(key.getPrivate());
		assertSameResult(Collections.emptyList(), List.of(c.get(0), c.get(1), late));
	}

	@Test
	void policyMismatch() {
		String served = "https://example.org/policy-stores/payments";
		for (TraceTestHarness harness : List.of(sequential, batch)) {
			harness.setPolicyMismatchCheck(PolicyMismatchChecks.serving(new PolicyMismatchChecks.StaticPolicyDownloadService()
					.archive(served, "1.2.3", "sha256:" + "f".repeat(64))));
		}
		assertSameResult(Collections.emptyList(), chain(CHAIN_1, 5));
		assertTrue(storedRecords(batch, knownRecordIds).stream().anyMatch(r -> r.getFlags().isPolicyMismatch()),
				"the scenario must flag at least one policy mismatch");
	}

	@Test
	void recordWithReservedMembers() {
		String reserved = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE)
				.producerChainId(CHAIN_1).genesis().withField("other-domain", "evidence_domain_id")
				.withField("attested", "trace", "trust_tier").sign(key.getPrivate());
		assertSameResult(Collections.emptyList(), List.of(reserved));
		assertTrue(storedRecords(batch, knownRecordIds).get(0).getFlags().isReservedMember());
	}

	@Test
	void twoChainsOfOneProducerInterleaved() {
		List<String> a = chain(CHAIN_1, 5);
		List<String> b = chain(CHAIN_2, 5);
		List<String> mixed = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			mixed.add(a.get(i));
			mixed.add(b.get(4 - i));
		}
		assertSameResult(Collections.emptyList(), mixed);
	}

	@Test
	void emptyAndAllFailingBatches() {
		assertSameResult(Collections.emptyList(), Collections.emptyList());
		List<String> c = chain(CHAIN_1, 2);
		assertSameResult(Collections.emptyList(), List.of(tampered(c.get(0)), tampered(c.get(1))));
	}

	// -- seeded random mixes ------------------------------------------------------------------

	@Test
	void seededRandomMixes() {
		for (long seed = 1; seed <= 12; seed++) {
			setUp();
			knownRecordIds.clear();
			randomScenario(seed);
		}
	}

	private void randomScenario(long seed) {
		knownRecordIds.clear();
		Random random = new Random(seed);
		List<String> pool = new ArrayList<>();
		List<String> c1 = chain(CHAIN_1, 8 + random.nextInt(25));
		List<String> c2 = chain(CHAIN_2, 8 + random.nextInt(25));
		pool.addAll(c1);
		pool.addAll(c2);
		// light shuffle: swap neighbours so arrival is mostly forward but partly out of order
		for (int i = 0; i < pool.size() / 2; i++) {
			int a = random.nextInt(pool.size());
			int b = Math.min(pool.size() - 1, a + 1 + random.nextInt(3));
			Collections.swap(pool, a, b);
		}

		List<String> preload = new ArrayList<>();
		List<String> elements = new ArrayList<>();
		for (String json : pool) {
			if (random.nextInt(10) == 0) {
				preload.add(json);
				continue;
			}
			elements.add(json);
		}

		int extras = 3 + random.nextInt(10);
		for (int i = 0; i < extras; i++) {
			String base = pool.get(random.nextInt(pool.size()));
			String variant;
			switch (random.nextInt(6)) {
			case 0:
				variant = base;
				break;
			case 1:
				variant = resignedWithOtherContent(base);
				break;
			case 2:
				variant = tampered(base);
				break;
			case 3:
				variant = record(CHAIN_UNREGISTERED, 1, TraceConstants.ZERO_HASH);
				break;
			case 4:
				variant = forkOf(base);
				break;
			default:
				variant = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE)
						.producerChainId(CHAIN_2).sequenceNumber(1 + random.nextInt(40))
						.prevRecordHash(TraceConstants.HASH_PREFIX + "3".repeat(64))
						.signedAt(TraceTestHarness.DEFAULT_NOW_MS / 1000 - 5_000).sign(key.getPrivate());
				break;
			}
			elements.add(random.nextInt(elements.size() + 1), variant);
		}
		assertTrue(elements.size() >= 16, "seed " + seed + " should build a real batch");
		assertSameResult(preload, elements);
	}

	private String forkOf(String json) {
		try {
			com.fasterxml.jackson.databind.JsonNode tree = MAPPER.readTree(json);
			com.fasterxml.jackson.databind.JsonNode chain = tree.path("producer_chain");
			return record(chain.path("producer_chain_id").asText(), chain.path("sequence_number").asLong(),
					chain.path("prev_record_hash").asText());
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
