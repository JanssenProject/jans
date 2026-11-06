/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.PolicyRecordQuery;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.store.TraceStoreContractTest;

/**
 * Runs the shared {@link TraceStoreContractTest} suite against {@link OrmTraceStore}, backed by
 * {@link FakeEntryManagerBehavior} instead of a live database.
 *
 * <p>Overrides the record fixture only to give each record a realistic {@code assertion} body: the
 * base fixture's {@code {"trace":{}}} placeholder has no {@code producer_chain} or
 * {@code trace.event} block, but {@code OrmTraceStore}'s read path recovers a record's
 * {@code producer_instance_id}/{@code producer_chain_id} and token references by
 * re-parsing the stored assertion (see {@link TraceEntityMapper}), because {@code TraceRecordEntry}
 * only ever stores their hashed keys.
 *
 * @author Yuriy Movchan
 */
class OrmTraceStoreTest extends TraceStoreContractTest {

	@Override
	protected TraceStore createStore() {
		return OrmTraceStores.create(FakeEntryManagerBehavior.create());
	}

	@Override
	protected StoredTraceRecord newRecord(String producerId, String recordId, String execAuthority, String execId,
			String chainProducerId, String chainInstanceId, String chainId, long seq, long receiptSeq,
			List<TokenRef> tokenRefs) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, producerId, recordId);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, execAuthority, execId);
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, chainProducerId, chainInstanceId, chainId);
		ChainPosition chainPosition = new ChainPosition(chainIdentity, seq);
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519");
		ReceiptEntry receipt = new ReceiptEntry(receiptSeq, 2000L + receiptSeq, TraceConstants.ZERO_HASH,
				"sha256:" + String.format("%064d", receiptSeq));
		IngestionFlags flags = new IngestionFlags(false, false, false, false);
		String assertion = buildAssertion(chainProducerId, chainInstanceId, chainId, seq, tokenRefs);
		return new StoredTraceRecord(identity, assertion, "sha256:" + String.format("%064d", receiptSeq), verification,
				receipt, flags, execution, chainPosition, TraceConstants.ZERO_HASH, tokenRefs,
				TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 3000L + receiptSeq, 0L);
	}

	/**
	 * A minimal but realistic assertion body carrying exactly the {@code producer_chain} and
	 * {@code trace.event} fields {@link TraceEntityMapper#toStoredTraceRecord} re-derives on read.
	 */
	private static String buildAssertion(String chainProducerId, String chainInstanceId, String chainId, long seq,
			List<TokenRef> tokenRefs) {
		StringBuilder tokensJson = new StringBuilder("[");
		for (int i = 0; i < tokenRefs.size(); i++) {
			if (i > 0) {
				tokensJson.append(",");
			}
			TokenRef ref = tokenRefs.get(i);
			tokensJson.append("{\"issuer\":\"").append(ref.getIssuer()).append("\",\"token_type\":\"")
					.append(ref.getTokenType()).append("\",");
			if (ref.getJti() != null) {
				tokensJson.append("\"jti\":\"").append(ref.getJti()).append("\"");
			} else {
				tokensJson.append("\"fingerprint\":\"").append(ref.getFingerprint()).append("\"");
			}
			tokensJson.append("}");
		}
		tokensJson.append("]");

		return "{\"trace\":{\"event\":{\"tokens\":" + tokensJson
				+ "}},\"producer_chain\":{\"producer_id\":\"" + chainProducerId + "\",\"producer_instance_id\":\""
				+ chainInstanceId + "\",\"producer_chain_id\":\"" + chainId + "\",\"sequence_number\":" + seq + "}}";
	}

	private static StoredTraceRecord inDomain(StoredTraceRecord r, String domain) {
		RecordIdentity id = new RecordIdentity(domain, r.getIdentity().getProducerId(), r.getIdentity().getRecordId());
		ExecutionIdentity exec = new ExecutionIdentity(domain, r.getExecution().getExecutionAuthority(),
				r.getExecution().getTraceExecutionId());
		ChainIdentity chain = r.getChainPosition().getChainIdentity();
		ChainPosition position = new ChainPosition(new ChainIdentity(domain, chain.getProducerId(),
				chain.getProducerInstanceId(), chain.getProducerChainId()), r.getChainPosition().getSequenceNumber());
		return new StoredTraceRecord(id, r.getAssertionRaw(), r.getContentDigest(), r.getVerification(),
				r.getReceipt(), r.getFlags(), exec, position, r.getPrevRecordHash(), r.getTokenRefs(),
				r.getEventKind(), r.getSignedAt(), r.getNodeId(), r.getCreatedAtMs(), r.getVersion(), r.getPolicy());
	}

	@Test
	void testFindRecordsByPolicy_caseInsensitiveDomainCollation_pagesOverExactDomainOnly() throws Exception {
		TraceStore collated = OrmTraceStores.create(FakeEntryManagerBehavior.createCaseInsensitiveDomain());
		PolicyRef policy = new PolicyRef("store-1", "1.0.0", "sha256:" + "1".repeat(64));
		// receipt sequences interleave the two spellings, so every raw page mixes both domains
		for (int i = 1; i <= 6; i++) {
			String domain = i % 2 == 0 ? DOMAIN : DOMAIN.toUpperCase();
			StoredTraceRecord base = newRecord("producer-1", "record-" + i, "authority-1", "exec-" + i, 1L, i);
			StoredTraceRecord withPolicy = new StoredTraceRecord(base.getIdentity(), base.getAssertionRaw(),
					base.getContentDigest(), base.getVerification(), base.getReceipt(), base.getFlags(),
					base.getExecution(), base.getChainPosition(), base.getPrevRecordHash(), base.getTokenRefs(),
					base.getEventKind(), base.getSignedAt(), base.getNodeId(), base.getCreatedAtMs(),
					base.getVersion(), policy);
			collated.insertRecord(inDomain(withPolicy, domain));
		}
		PolicyRecordQuery query = new PolicyRecordQuery("store-1", null, null, null, null);

		assertEquals(Arrays.asList("record-2"), recordIds(collated.findRecordsByPolicy(DOMAIN, query, 0, 1)));
		assertEquals(Arrays.asList("record-4"), recordIds(collated.findRecordsByPolicy(DOMAIN, query, 1, 1)));
		assertEquals(Arrays.asList("record-4", "record-6"),
				recordIds(collated.findRecordsByPolicy(DOMAIN, query, 1, 5)));
		assertTrue(collated.findRecordsByPolicy(DOMAIN, query, 3, 1).isEmpty());
	}

	private static List<String> recordIds(List<StoredTraceRecord> records) {
		return records.stream().map(r -> r.getIdentity().getRecordId()).collect(java.util.stream.Collectors.toList());
	}

}
