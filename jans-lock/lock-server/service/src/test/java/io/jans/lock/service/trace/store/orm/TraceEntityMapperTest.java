/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceKeys;

/**
 * Tests for {@link TraceEntityMapper}: the record round trip (including the assertion re-parse that
 * recovers {@code producer_instance_id}/{@code producer_chain_id}, capability ids and token
 * references — see task 13's design decision on entity-to-record mapping) and the
 * null-Boolean-flag convention. Receipts, chains and producer keys need no mapper test: since the
 * collapse of their store value types onto the jans-orm entities, {@code TraceStore} exposes those
 * entities directly and there is no conversion left to test here.
 *
 * @author Yuriy Movchan
 */
class TraceEntityMapperTest {

	private static final String DOMAIN = "default";

	private static final String ASSERTION = "{\"trace\":{\"event\":{\"capability_ids\":"
			+ "[{\"capability_id\":\"cap-1\"},{\"capability_id\":\"cap-2\"}],\"tokens\":"
			+ "[{\"issuer\":\"https://issuer.example.org\",\"token_type\":\"access_token\",\"jti\":\"jti-1\"}]}},"
			+ "\"producer_chain\":{\"producer_id\":\"producer-1\",\"producer_instance_id\":\"instance-1\","
			+ "\"producer_chain_id\":\"chain-1\",\"sequence_number\":7}}";

	private static StoredTraceRecord newRecord(String assertion) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, "producer-1", "record-1");
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, "authority-1", "exec-1");
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		ChainPosition chainPosition = new ChainPosition(chainIdentity, 7L);
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519");
		ReceiptEntry receipt = new ReceiptEntry(42L, 5000L, TraceConstants.ZERO_HASH, "sha256:" + "1".repeat(64));
		IngestionFlags flags = new IngestionFlags(true, false, true, false);
		return new StoredTraceRecord(identity, assertion, "sha256:" + "2".repeat(64), verification, receipt, flags,
				execution, chainPosition, TraceConstants.ZERO_HASH, Arrays.asList("cap-1", "cap-2"),
				Collections.singletonList(new TokenRef("https://issuer.example.org", "access_token", "jti-1", null)),
				TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 9000L);
	}

	@Test
	void testToEntity_thenToStoredTraceRecord_roundTripsIdentityAndCoreFields() {
		StoredTraceRecord record = newRecord(ASSERTION);

		TraceRecordEntry entity = TraceEntityMapper.toEntity(record, "jansId=x,ou=records,ou=trace,ou=lock,o=jans");
		StoredTraceRecord roundTripped = TraceEntityMapper.toStoredTraceRecord(entity);

		assertEquals(record.getIdentity(), roundTripped.getIdentity());
		assertEquals(record.getContentDigest(), roundTripped.getContentDigest());
		assertEquals(record.getAssertionRaw(), roundTripped.getAssertionRaw());
		assertEquals(record.getExecution(), roundTripped.getExecution());
		assertEquals(record.getChainPosition().getChainIdentity(), roundTripped.getChainPosition().getChainIdentity());
		assertEquals(record.getChainPosition().getSequenceNumber(), roundTripped.getChainPosition().getSequenceNumber());
		assertEquals(record.getCapabilityIds(), roundTripped.getCapabilityIds());
		assertEquals(record.getTokenRefs(), roundTripped.getTokenRefs());
		assertEquals(record.getEventKind(), roundTripped.getEventKind());
		assertEquals(record.getSignedAt(), roundTripped.getSignedAt());
		assertEquals(record.getNodeId(), roundTripped.getNodeId());
		assertEquals(record.getCreatedAtMs(), roundTripped.getCreatedAtMs());
		assertEquals(record.getVerification(), roundTripped.getVerification());
		assertEquals(record.getFlags(), roundTripped.getFlags());
	}

	@Test
	void testToEntity_computesHashedKeysFromRawValues() {
		StoredTraceRecord record = newRecord(ASSERTION);

		TraceRecordEntry entity = TraceEntityMapper.toEntity(record, "jansId=x,ou=records,ou=trace,ou=lock,o=jans");

		assertEquals(TraceKeys.recordKey(record.getIdentity()), entity.getId());
		assertEquals(TraceKeys.chainKey(record.getChainPosition().getChainIdentity()), entity.getChainKey());
		assertEquals(TraceKeys.chainPositionKey(record.getChainPosition()), entity.getChainPosKey());
		assertEquals(Arrays.asList(TraceKeys.capabilityKey(DOMAIN, "cap-1"), TraceKeys.capabilityKey(DOMAIN, "cap-2")),
				entity.getCapKeys());
	}

	@Test
	void testToStoredTraceRecord_missingProducerChainField_throwsTraceStorageException() {
		TraceRecordEntry entity = TraceEntityMapper.toEntity(newRecord(ASSERTION),
				"jansId=x,ou=records,ou=trace,ou=lock,o=jans");
		entity.setAssertion("{\"producer_chain\":{}}");

		assertThrows(TraceStorageException.class, () -> TraceEntityMapper.toStoredTraceRecord(entity));
	}

	@Test
	void testToStoredTraceRecord_nullBooleanFlags_treatedAsFalse() {
		TraceRecordEntry entity = TraceEntityMapper.toEntity(newRecord(ASSERTION),
				"jansId=x,ou=records,ou=trace,ou=lock,o=jans");
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
