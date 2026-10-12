/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.service.trace.canon.CanonicalHashes;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.receipt.ChainVerificationReport;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestHarness;

/**
 * Task 22 deliverable 4: after a fixed-seed randomized sequence of 50 ingestions across two
 * domains and two producers per domain (forward chain positions in shuffled arrival order — which
 * routinely produces out-of-order chain positions — two replays, a same-identity conflict and one
 * forced storage failure), {@link io.jans.lock.service.trace.receipt.TraceReceiptChain#verifyDomain}
 * reports OK and dense for both domains.
 *
 * <p>The receipt chain (design §9, D-8) is keyed only by {@code (domainId, receipt_sequence)} and
 * is unaffected by producer-chain-level coverage gaps: neither a replay nor a same-identity
 * conflict ever claims a receipt — both are caught by {@code TraceIngestionService}'s
 * identity pre-check (before {@code TraceReceiptChain#claim}), the same short-circuit whether the
 * resubmitted content matches or not. Only the forced {@code insertRecord} failure reaches
 * allocation: it claims a receipt and then voids it (D-8 step 5; the cross-node
 * {@code DuplicateEntryException} race path voids the same way but is not exercised here, a single
 * JVM). So the receipt sequence stays exactly as dense as the number of distinct identities
 * attempted, regardless of producer-chain arrival order — which is what lets this test fully
 * shuffle the 48 forward positions across 4 independent producer chains and still assert an exact
 * head sequence.
 */
class TraceChainIntegrityTest {

	private static final long SEED = 42L;

	private static final int POSITIONS_PER_CHAIN = 12;

	private static final String DOMAIN_A = "domain-a";

	private static final String DOMAIN_B = "domain-b";

	private static final String PRODUCER = "cedarling-fleet-1/1.0.0";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN = "chain-01JABC9Z0K";

	private static final String GW_PRODUCER = "gateway-fleet-1/2.0.0";

	private static final String GW_KID = "gateway-fleet-1-2026-01";

	private static final String GW_INSTANCE = "gateway-001";

	private static final String GW_CHAIN = "chain-02JABC9Z0K";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TraceTestHarness harness;

	@BeforeEach
	void setUp() {
		harness = new TraceTestHarness();
	}

	private static String contentDigestOf(String json) {
		try {
			JsonNode tree = MAPPER.readTree(json);
			return CanonicalHashes.sha256PrefixedOfJcs(tree);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/** One signed assertion per {@code sequence_number} 1..{@link #POSITIONS_PER_CHAIN}, properly chained. */
	private static List<String> buildChain(Supplier<TraceRecordBuilder> fixtureSupplier, KeyPair keyPair, int count) {
		List<String> jsons = new ArrayList<>();
		String prevHash = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= count; seq++) {
			String json = fixtureSupplier.get().sequenceNumber(seq).prevRecordHash(prevHash).sign(keyPair.getPrivate());
			jsons.add(json);
			prevHash = contentDigestOf(json);
		}
		return jsons;
	}

	private static final class Op {

		final String domainId;

		final String json;

		Op(String domainId, String json) {
			this.domainId = domainId;
			this.json = json;
		}
	}

	@Test
	void chainIntegrity_fiftyRandomizedIngestions_bothDomainsVerifyOkAndDense() {
		KeyPair keyA0 = harness.registerKey(DOMAIN_A, PRODUCER, KID);
		harness.registerChain(DOMAIN_A, PRODUCER, INSTANCE, CHAIN);
		KeyPair keyA1 = harness.registerKey(DOMAIN_A, GW_PRODUCER, GW_KID);
		harness.registerChain(DOMAIN_A, GW_PRODUCER, GW_INSTANCE, GW_CHAIN);
		KeyPair keyB0 = harness.registerKey(DOMAIN_B, PRODUCER, KID);
		harness.registerChain(DOMAIN_B, PRODUCER, INSTANCE, CHAIN);
		KeyPair keyB1 = harness.registerKey(DOMAIN_B, GW_PRODUCER, GW_KID);
		harness.registerChain(DOMAIN_B, GW_PRODUCER, GW_INSTANCE, GW_CHAIN);

		List<String> chainA0 = buildChain(TraceRecordBuilder::authorizationDecision, keyA0, POSITIONS_PER_CHAIN);
		List<String> chainA1 = buildChain(TraceRecordBuilder::capabilityInvoked, keyA1, POSITIONS_PER_CHAIN);
		List<String> chainB0 = buildChain(TraceRecordBuilder::authorizationDecision, keyB0, POSITIONS_PER_CHAIN);
		List<String> chainB1 = buildChain(TraceRecordBuilder::capabilityInvoked, keyB1, POSITIONS_PER_CHAIN);

		List<Op> forward = new ArrayList<>();
		addAll(forward, DOMAIN_A, chainA0);
		addAll(forward, DOMAIN_A, chainA1);
		addAll(forward, DOMAIN_B, chainB0);
		addAll(forward, DOMAIN_B, chainB1);
		assertEquals(48, forward.size());
		Collections.shuffle(forward, new Random(SEED));

		// The last position of chainA0 is deliberately lost to a storage failure and never retried:
		// receipt density does not depend on it (D-8 step 5 voids the claimed receipt either way).
		String forcedFailureJson = chainA0.get(POSITIONS_PER_CHAIN - 1);

		// Two replays, of earlier chainA1 and chainB1 positions: the identity pre-check short-circuits
		// before any receipt is claimed (TraceIngestionService#ingest), so neither affects density.
		String replayJsonA = chainA1.get(2);
		String replayJsonB = chainB1.get(6);

		// A same-identity, different-content resubmission of a chainB0 position: the same identity
		// pre-check catches it first (no receipt claimed either), then 409s on the digest mismatch.
		String conflictBaseJson = chainB0.get(4);
		String conflictJson = TraceRecordBuilder.fromJson(conflictBaseJson)
				.withField("9.9.9-conflict", "trace", "policy", "policy_store_version").sign(keyB0.getPrivate());

		int succeeded = 0;
		int forcedFailures = 0;
		boolean sawCoverageGap = false;
		for (Op op : forward) {
			if (op.json.equals(forcedFailureJson)) {
				harness.store().failNextInsertRecord();
				assertThrows(TraceStorageException.class, () -> harness.ingest(op.json, harness.ctx(op.domainId)));
				forcedFailures++;
				continue;
			}
			AcceptanceResult result = harness.ingest(op.json, harness.ctx(op.domainId));
			assertTrue(result.isAccepted());
			sawCoverageGap = sawCoverageGap || result.isCoverageGapFlag();
			succeeded++;
		}
		assertEquals(1, forcedFailures);
		assertEquals(47, succeeded);
		assertTrue(sawCoverageGap, "shuffling 48 positions across 4 chains must produce at least one "
				+ "out-of-order (coverage-gap) arrival for seed " + SEED);

		AcceptanceResult replayA = harness.ingest(replayJsonA, harness.ctx(DOMAIN_A));
		assertTrue(replayA.isIdempotentReplay());
		AcceptanceResult replayB = harness.ingest(replayJsonB, harness.ctx(DOMAIN_B));
		assertTrue(replayB.isIdempotentReplay());

		TraceConflictException ex = assertThrows(TraceConflictException.class,
				() -> harness.ingest(conflictJson, harness.ctx(DOMAIN_B)));
		assertEquals(io.jans.lock.model.error.TraceErrorResponseType.RECORD_CONFLICT, ex.getErrorId());

		ChainVerificationReport reportA = harness.verifyDomain(DOMAIN_A);
		ChainVerificationReport reportB = harness.verifyDomain(DOMAIN_B);
		assertTrue(reportA.isOk(), () -> "domain-a report: " + reportA);
		assertTrue(reportB.isOk(), () -> "domain-b report: " + reportB);
		assertEquals(reportA.getHeadSequence(), reportA.getEntries().size(), "domain-a receipt sequence must be dense");
		assertEquals(reportB.getHeadSequence(), reportB.getEntries().size(), "domain-b receipt sequence must be dense");
		// 24 attempts per domain (2 chains x 12 positions) claim a receipt each; replays and the
		// conflict are all caught by the identity pre-check and claim none.
		assertEquals(24, reportA.getHeadSequence());
		assertEquals(24, reportB.getHeadSequence());
	}

	private static void addAll(List<Op> ops, String domainId, List<String> jsons) {
		for (String json : jsons) {
			ops.add(new Op(domainId, json));
		}
	}

}
