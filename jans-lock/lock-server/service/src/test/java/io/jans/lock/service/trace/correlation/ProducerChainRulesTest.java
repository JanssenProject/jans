/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;

/**
 * Pure unit tests for {@link ProducerChainRules}: no store, no CDI (design §8, §12, design
 * decisions D-9, D-11).
 */
class ProducerChainRulesTest {

	private static final String HASH_A = "sha256:" + "a".repeat(64);

	private static final String HASH_B = "sha256:" + "b".repeat(64);

	private static StoredTraceRecord recordWithDigest(String recordId, String contentDigest) {
		return StoredTraceRecordFactory.builder().recordId(recordId).contentDigest(contentDigest).build();
	}

	// -- isCoverageGap ----------------------------------------------------------------------------

	@Test
	void testIsCoverageGap_Genesis_AlwaysFalse() {
		assertFalse(ProducerChainRules.isCoverageGap(1L, Collections.emptyList()));
	}

	@Test
	void testIsCoverageGap_NonGenesisEmptyPredecessors_True() {
		assertTrue(ProducerChainRules.isCoverageGap(2L, Collections.emptyList()));
	}

	@Test
	void testIsCoverageGap_NonGenesisNonEmptyPredecessors_False() {
		List<StoredTraceRecord> predecessors = Collections.singletonList(recordWithDigest("r1", HASH_A));
		assertFalse(ProducerChainRules.isCoverageGap(2L, predecessors));
	}

	// -- isChainLinkFailure -------------------------------------------------------------------------

	@Test
	void testIsChainLinkFailure_Genesis_AlwaysFalse() {
		assertFalse(ProducerChainRules.isChainLinkFailure(1L, Collections.emptyList(), TraceConstants.ZERO_HASH));
	}

	@Test
	void testIsChainLinkFailure_EmptyPredecessors_False() {
		assertFalse(ProducerChainRules.isChainLinkFailure(2L, Collections.emptyList(), HASH_A));
	}

	@Test
	void testIsChainLinkFailure_MatchingDigest_False() {
		List<StoredTraceRecord> predecessors = Collections.singletonList(recordWithDigest("r1", HASH_A));
		assertFalse(ProducerChainRules.isChainLinkFailure(2L, predecessors, HASH_A));
	}

	@Test
	void testIsChainLinkFailure_AnyMatchAmongEquivocatedPredecessors_False() {
		List<StoredTraceRecord> predecessors = Arrays.asList(recordWithDigest("r1", HASH_B),
				recordWithDigest("r2", HASH_A));
		assertFalse(ProducerChainRules.isChainLinkFailure(2L, predecessors, HASH_A));
	}

	@Test
	void testIsChainLinkFailure_NoMatchingDigest_True() {
		List<StoredTraceRecord> predecessors = Collections.singletonList(recordWithDigest("r1", HASH_A));
		assertTrue(ProducerChainRules.isChainLinkFailure(2L, predecessors, HASH_B));
	}

	// -- equivocationPeers --------------------------------------------------------------------------

	@Test
	void testEquivocationPeers_NoOtherRows_Empty() {
		RecordIdentity thisIdentity = new RecordIdentity("domain-1", "producer-1", "r1");
		assertTrue(ProducerChainRules.equivocationPeers(thisIdentity, Collections.emptyList()).isEmpty());
	}

	@Test
	void testEquivocationPeers_ExcludesSameIdentity() {
		RecordIdentity thisIdentity = new RecordIdentity("domain-1", "producer-1", "r1");
		StoredTraceRecord sameIdentityRow = StoredTraceRecordFactory.builder().domainId("domain-1")
				.producerId("producer-1").recordId("r1").contentDigest(HASH_A).build();

		assertTrue(ProducerChainRules.equivocationPeers(thisIdentity, Collections.singletonList(sameIdentityRow))
				.isEmpty());
	}

	@Test
	void testEquivocationPeers_IncludesDifferentIdentities() {
		RecordIdentity thisIdentity = new RecordIdentity("domain-1", "producer-1", "r1");
		StoredTraceRecord peer = StoredTraceRecordFactory.builder().domainId("domain-1").producerId("producer-1")
				.recordId("r2").contentDigest(HASH_B).build();

		List<RecordIdentity> peers = ProducerChainRules.equivocationPeers(thisIdentity,
				Collections.singletonList(peer));

		assertEquals(1, peers.size());
		assertEquals(peer.getIdentity(), peers.get(0));
	}

	// -- isLate ---------------------------------------------------------------------------------------

	@Test
	void testIsLate_OlderThanThreshold_True() {
		assertTrue(ProducerChainRules.isLate(2_000_000L, 1_000L, 300));
	}

	@Test
	void testIsLate_WithinThreshold_False() {
		assertFalse(ProducerChainRules.isLate(1_200_000L, 1_000L, 300));
	}

	@Test
	void testIsLate_ExactlyAtThreshold_False() {
		assertFalse(ProducerChainRules.isLate(1_300_000L, 1_000L, 300));
	}

	@Test
	void testIsLate_SignedAtInFuture_False() {
		assertFalse(ProducerChainRules.isLate(1_000_000L, 2_000L, 300));
	}

	// -- successorLinkBroken --------------------------------------------------------------------------

	@Test
	void testSuccessorLinkBroken_Matching_False() {
		assertFalse(ProducerChainRules.successorLinkBroken(HASH_A, HASH_A));
	}

	@Test
	void testSuccessorLinkBroken_Mismatch_True() {
		assertTrue(ProducerChainRules.successorLinkBroken(HASH_A, HASH_B));
	}

	// -- constantTimeEquals -----------------------------------------------------------------------------

	@Test
	void testConstantTimeEquals_EqualStrings_True() {
		assertTrue(ProducerChainRules.constantTimeEquals(HASH_A, "sha256:" + "a".repeat(64)));
	}

	@Test
	void testConstantTimeEquals_DifferentStrings_False() {
		assertFalse(ProducerChainRules.constantTimeEquals(HASH_A, HASH_B));
	}

	@Test
	void testConstantTimeEquals_DifferentLengths_False() {
		assertFalse(ProducerChainRules.constantTimeEquals(HASH_A, "sha256:short"));
	}

	@Test
	void testConstantTimeEquals_BothNull_True() {
		assertTrue(ProducerChainRules.constantTimeEquals(null, null));
	}

	@Test
	void testConstantTimeEquals_OneNull_False() {
		assertFalse(ProducerChainRules.constantTimeEquals(null, HASH_A));
	}

}
