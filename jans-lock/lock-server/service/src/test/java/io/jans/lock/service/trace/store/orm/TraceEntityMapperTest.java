/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.model.trace.entity.TraceVerification;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;

/**
 * Tests for {@link TraceEntityMapper}: the record round trip (including the assertion re-parse that
 * recovers {@code producer_instance_id}/{@code producer_chain_id} and token
 * references) and the
 * null-Boolean-flag convention. Receipts, chains and producer keys need no mapper test: since the
 * collapse of their store value types onto the jans-orm entities, {@code TraceStore} exposes those
 * entities directly and there is no conversion left to test here.
 *
 * @author Yuriy Movchan
 */
class TraceEntityMapperTest {

	private static final String DOMAIN = "default";

	private static final TraceKeys TRACE_KEYS = TraceTestCrypto.traceKeys();

	private static final String ASSERTION = "{\"trace\":{\"event\":{\"capability_ids\":"
			+ "[{\"capability_id\":\"cap-1\"},{\"capability_id\":\"cap-2\"}],\"tokens\":"
			+ "[{\"issuer\":\"https://issuer.example.org\",\"token_type\":\"access_token\",\"jti\":\"jti-1\"}]}},"
			+ "\"producer_chain\":{\"producer_id\":\"producer-1\",\"producer_instance_id\":\"instance-1\","
			+ "\"producer_chain_id\":\"chain-1\",\"sequence_number\":7}}";

	private static StoredTraceRecord newRecord(String assertion) {
		return newRecord(assertion, new VerificationResult(true, "kid-1", 1000L, "Ed25519"));
	}

	private static StoredTraceRecord newRecord(String assertion, VerificationResult verification) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, "producer-1", "record-1");
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, "authority-1", "exec-1");
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		ChainPosition chainPosition = new ChainPosition(chainIdentity, 7L);
		ReceiptEntry receipt = new ReceiptEntry(42L, 5000L, TraceConstants.ZERO_HASH, "sha256:" + "1".repeat(64));
		IngestionFlags flags = new IngestionFlags(true, false, true, false);
		return new StoredTraceRecord(identity, assertion, "sha256:" + "2".repeat(64), verification, receipt, flags,
				execution, chainPosition, TraceConstants.ZERO_HASH,
				Collections.singletonList(new TokenRef("https://issuer.example.org", "access_token", "jti-1", null)),
				TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 9000L, 7L);
	}

	@Test
	void testToEntity_thenToStoredTraceRecord_roundTripsIdentityAndCoreFields() {
		StoredTraceRecord stored = newRecord(ASSERTION);

		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		StoredTraceRecord roundTripped = TraceEntityMapper.toStoredTraceRecord(entity);

		assertEquals(stored.getIdentity(), roundTripped.getIdentity());
		assertEquals(stored.getContentDigest(), roundTripped.getContentDigest());
		assertEquals(stored.getAssertionRaw(), roundTripped.getAssertionRaw());
		assertEquals(stored.getExecution(), roundTripped.getExecution());
		assertEquals(stored.getChainPosition().getChainIdentity(), roundTripped.getChainPosition().getChainIdentity());
		assertEquals(stored.getChainPosition().getSequenceNumber(), roundTripped.getChainPosition().getSequenceNumber());
		assertEquals(stored.getTokenRefs(), roundTripped.getTokenRefs());
		assertEquals(stored.getEventKind(), roundTripped.getEventKind());
		assertEquals(stored.getSignedAt(), roundTripped.getSignedAt());
		assertEquals(stored.getNodeId(), roundTripped.getNodeId());
		assertEquals(stored.getCreatedAtMs(), roundTripped.getCreatedAtMs());
		assertEquals(stored.getVerification(), roundTripped.getVerification());
		assertEquals(stored.getFlags(), roundTripped.getFlags());
		assertEquals(stored.getVersion(), roundTripped.getVersion());
	}

	@Test
	void testToEntity_policyReleaseAndMismatchFlag_roundTrip() {
		StoredTraceRecord base = newRecord(ASSERTION);
		PolicyRef policy = new PolicyRef("store-1", "1.2.3", "sha256:" + "c".repeat(64));
		StoredTraceRecord stored = new StoredTraceRecord(base.getIdentity(), base.getAssertionRaw(),
				base.getContentDigest(), base.getVerification(), base.getReceipt(),
				new IngestionFlags(false, false, false, false, true), base.getExecution(), base.getChainPosition(),
				base.getPrevRecordHash(), base.getTokenRefs(), base.getEventKind(), base.getSignedAt(),
				base.getNodeId(), base.getCreatedAtMs(), base.getVersion(), policy);

		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		StoredTraceRecord roundTripped = TraceEntityMapper.toStoredTraceRecord(entity);

		assertEquals("store-1", entity.getPolicyStoreId());
		assertEquals("1.2.3", entity.getPolicyStoreVersion());
		assertEquals("sha256:" + "c".repeat(64), entity.getBundleHash());
		assertEquals(Boolean.TRUE, entity.getPolicyMismatch());
		assertEquals(policy, roundTripped.getPolicy());
		assertTrue(roundTripped.getFlags().isPolicyMismatch());
	}

	@Test
	void testToEntity_reservedMembers_roundTrip() {
		StoredTraceRecord base = newRecord(ASSERTION);
		IngestionFlags flags = new IngestionFlags(false, false, false, false);
		flags.setReservedMembers(List.of("evidence_domain_id", "trace.trust_tier"));
		StoredTraceRecord stored = base.withFlags(flags, base.getVersion());

		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		StoredTraceRecord roundTripped = TraceEntityMapper.toStoredTraceRecord(entity);

		assertEquals(List.of("evidence_domain_id", "trace.trust_tier"), entity.getReservedMembers());
		assertEquals(flags.getReservedMembers(), roundTripped.getFlags().getReservedMembers());
		assertTrue(roundTripped.getFlags().isReservedMember());

		entity.setReservedMembers(null);
		assertFalse(TraceEntityMapper.toStoredTraceRecord(entity).getFlags().isReservedMember());
	}

	@Test
	void testToEntity_noPolicy_leavesPolicyAttributesUnset() {
		TraceRecordEntry entity = TraceEntityMapper.toEntity(newRecord(ASSERTION), "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);

		assertNull(entity.getPolicyStoreId());
		assertNull(entity.getBundleHash());
		assertNull(TraceEntityMapper.toStoredTraceRecord(entity).getPolicy());
		entity.setPolicyMismatch(null);
		assertFalse(TraceEntityMapper.toStoredTraceRecord(entity).getFlags().isPolicyMismatch());
	}

	@Test
	void testToEntity_verificationBlockWithThumbprintAndKeyAuthorized_roundTrips() {
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519",
				"sha256:" + "a".repeat(64), Boolean.TRUE);
		StoredTraceRecord stored = newRecord(ASSERTION, verification);

		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		StoredTraceRecord roundTripped = TraceEntityMapper.toStoredTraceRecord(entity);

		assertEquals("sha256:" + "a".repeat(64), entity.getVerification().getKeyThumbprint());
		assertEquals(Boolean.TRUE, entity.getVerification().getKeyAuthorized());
		assertEquals(verification, roundTripped.getVerification());
	}

	@Test
	void testToStoredTraceRecord_legacyVerificationWithoutNewMembers_mapsWithNulls() {
		StoredTraceRecord stored = newRecord(ASSERTION);
		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		TraceVerification legacy = new TraceVerification();
		legacy.setSignatureValid(true);
		legacy.setKeyId("kid-1");
		legacy.setVerifiedAtMs(1000L);
		legacy.setAlgorithm("Ed25519");
		entity.setVerification(legacy);

		VerificationResult mapped = TraceEntityMapper.toStoredTraceRecord(entity).getVerification();

		assertNull(mapped.getKeyThumbprint());
		assertNull(mapped.getKeyAuthorized());
		assertEquals("kid-1", mapped.getKeyId());
	}

	@Test
	void testToEntity_computesHashedKeysFromRawValues() {
		StoredTraceRecord stored = newRecord(ASSERTION);

		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, "jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);

		assertEquals(TRACE_KEYS.recordKey(stored.getIdentity()), entity.getId());
		assertEquals(TRACE_KEYS.chainKey(stored.getChainPosition().getChainIdentity()), entity.getChainKey());
		assertEquals(TRACE_KEYS.chainPositionKey(stored.getChainPosition()), entity.getChainPosKey());
		assertEquals(Arrays.asList(TRACE_KEYS.tokenKey(DOMAIN, stored.getTokenRefs().get(0))), entity.getTokenKeys());
	}

	@Test
	void testToStoredTraceRecord_missingProducerChainField_throwsTraceStorageException() {
		TraceRecordEntry entity = TraceEntityMapper.toEntity(newRecord(ASSERTION),
				"jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		entity.setAssertion("{\"producer_chain\":{}}");

		assertThrows(TraceStorageException.class, () -> TraceEntityMapper.toStoredTraceRecord(entity));
	}

	@Test
	void testToStoredTraceRecord_nullBooleanFlags_treatedAsFalse() {
		TraceRecordEntry entity = TraceEntityMapper.toEntity(newRecord(ASSERTION),
				"jansId=x,ou=records,ou=trace,ou=lock,o=jans", TRACE_KEYS);
		entity.setCoverageGap(null);
		entity.setChainLinkFailure(null);
		entity.setEquivocation(null);
		entity.setLate(null);

		StoredTraceRecord roundTripped = TraceEntityMapper.toStoredTraceRecord(entity);

		assertFalse(roundTripped.getFlags().isCoverageGap());
		assertFalse(roundTripped.getFlags().isChainLinkFailure());
		assertFalse(roundTripped.getFlags().isEquivocation());
		assertFalse(roundTripped.getFlags().isLate());
	}

}
