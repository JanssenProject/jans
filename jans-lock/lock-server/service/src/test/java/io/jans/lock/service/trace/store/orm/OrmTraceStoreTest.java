/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.List;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.store.TraceStoreContractTest;

/**
 * Runs the shared {@link TraceStoreContractTest} suite against {@link OrmTraceStore}, backed by
 * {@link FakeEntryManagerBehavior} instead of a live database (task 13 acceptance criteria).
 *
 * <p>Overrides the record fixture only to give each record a realistic {@code assertion} body: the
 * base fixture's {@code {"trace":{}}} placeholder has no {@code producer_chain} or
 * {@code trace.event} block, but {@code OrmTraceStore}'s read path recovers a record's
 * {@code producer_instance_id}/{@code producer_chain_id}, capability ids and token references by
 * re-parsing the stored assertion (see {@link TraceEntityMapper}), because {@code TraceRecordEntry}
 * (T-1) only ever stores their hashed keys.
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
			List<String> capabilityIds, List<TokenRef> tokenRefs) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, producerId, recordId);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, execAuthority, execId);
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, chainProducerId, chainInstanceId, chainId);
		ChainPosition chainPosition = new ChainPosition(chainIdentity, seq);
		VerificationResult verification = new VerificationResult(true, "kid-1", 1000L, "Ed25519");
		ReceiptEntry receipt = new ReceiptEntry(receiptSeq, 2000L + receiptSeq, TraceConstants.ZERO_HASH,
				"sha256:" + String.format("%064d", receiptSeq));
		IngestionFlags flags = new IngestionFlags(false, false, false, false);
		String assertion = buildAssertion(chainProducerId, chainInstanceId, chainId, seq, capabilityIds, tokenRefs);
		return new StoredTraceRecord(identity, assertion, "sha256:" + String.format("%064d", receiptSeq), verification,
				receipt, flags, execution, chainPosition, TraceConstants.ZERO_HASH, capabilityIds, tokenRefs,
				TraceConstants.EVENT_KIND_CAPABILITY_INVOKED, 500L, "node-1", 3000L + receiptSeq);
	}

	/**
	 * A minimal but realistic assertion body carrying exactly the {@code producer_chain} and
	 * {@code trace.event} fields {@link TraceEntityMapper#toStoredTraceRecord} re-derives on read.
	 */
	private static String buildAssertion(String chainProducerId, String chainInstanceId, String chainId, long seq,
			List<String> capabilityIds, List<TokenRef> tokenRefs) {
		StringBuilder capabilitiesJson = new StringBuilder("[");
		for (int i = 0; i < capabilityIds.size(); i++) {
			if (i > 0) {
				capabilitiesJson.append(",");
			}
			capabilitiesJson.append("{\"capability_id\":\"").append(capabilityIds.get(i)).append("\"}");
		}
		capabilitiesJson.append("]");

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

		return "{\"trace\":{\"event\":{\"capability_ids\":" + capabilitiesJson + ",\"tokens\":" + tokensJson
				+ "}},\"producer_chain\":{\"producer_id\":\"" + chainProducerId + "\",\"producer_instance_id\":\""
				+ chainInstanceId + "\",\"producer_chain_id\":\"" + chainId + "\",\"sequence_number\":" + seq + "}}";
	}

}
