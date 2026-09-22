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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ChainRegistration;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.ReceiptRow;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceKeys;

/**
 * Tests for {@link TraceEntityMapper}: the record round trip (including the assertion re-parse that
 * recovers {@code producer_instance_id}/{@code producer_chain_id}, capability ids and token
 * references — see task 13's design decision on entity-to-record mapping), the null-Boolean-flag
 * convention, and the chain/receipt/producer-key mappings.
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

	@Test
	void testChainRegistration_roundTrip() {
		ChainIdentity identity = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		ChainRegistration registration = new ChainRegistration(identity, 12345L, "client-1");

		TraceChainEntry entity = TraceEntityMapper.toEntity(registration, "jansId=x,ou=chains,ou=trace,ou=lock,o=jans");
		ChainRegistration roundTripped = TraceEntityMapper.toChainRegistration(entity);

		assertEquals(registration.getChainIdentity(), roundTripped.getChainIdentity());
		assertEquals(registration.getRegisteredAtMs(), roundTripped.getRegisteredAtMs());
		assertEquals(registration.getRegisteredBy(), roundTripped.getRegisteredBy());
	}

	@Test
	void testProducerKey_roundTrip() {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");
		ProducerKey key = new ProducerKey(DOMAIN, "producer-1", "kid-1", jwk, 100L, 200L, 150L, "client-1");

		TraceProducerKeyEntry entity = TraceEntityMapper.toEntity(key, "jansId=x,ou=keys,ou=trace,ou=lock,o=jans");
		ProducerKey roundTripped = TraceEntityMapper.toProducerKey(entity);

		assertEquals(key.getDomainId(), roundTripped.getDomainId());
		assertEquals(key.getProducerId(), roundTripped.getProducerId());
		assertEquals(key.getKid(), roundTripped.getKid());
		assertEquals(key.getPublicKeyJwk(), roundTripped.getPublicKeyJwk());
		assertEquals(key.getValidFromMs(), roundTripped.getValidFromMs());
		assertEquals(key.getValidUntilMs(), roundTripped.getValidUntilMs());
		assertEquals(key.getRevokedAtMs(), roundTripped.getRevokedAtMs());
		assertEquals(key.getRegisteredBy(), roundTripped.getRegisteredBy());
	}

	@Test
	void testReceiptRow_roundTrip() {
		ReceiptRow row = new ReceiptRow(DOMAIN, 5L, 6000L, "producer-1", "record-1",
				TraceKeys.recordKey(new RecordIdentity(DOMAIN, "producer-1", "record-1")), "sha256:" + "3".repeat(64),
				TraceConstants.ZERO_HASH, "sha256:" + "4".repeat(64), TraceReceiptState.COMMITTED, "node-1");

		TraceReceiptEntry entity = TraceEntityMapper.toEntity(row, "jansId=x,ou=receipts,ou=trace,ou=lock,o=jans");
		ReceiptRow roundTripped = TraceEntityMapper.toReceiptRow(entity);

		assertEquals(row.getDomainId(), roundTripped.getDomainId());
		assertEquals(row.getReceiptSequence(), roundTripped.getReceiptSequence());
		assertEquals(row.getReceivedAtMs(), roundTripped.getReceivedAtMs());
		assertEquals(row.getProducerId(), roundTripped.getProducerId());
		assertEquals(row.getRecordId(), roundTripped.getRecordId());
		assertEquals(row.getRecordKey(), roundTripped.getRecordKey());
		assertEquals(row.getContentDigest(), roundTripped.getContentDigest());
		assertEquals(row.getPrevReceiptHash(), roundTripped.getPrevReceiptHash());
		assertEquals(row.getReceiptHash(), roundTripped.getReceiptHash());
		assertEquals(row.getState(), roundTripped.getState());
		assertEquals(row.getNodeId(), roundTripped.getNodeId());
	}

}
