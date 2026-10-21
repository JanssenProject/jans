/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.model.trace.entity.TraceVerification;
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

/**
 * Pure mapping between the {@link StoredTraceRecord} value type and the {@link TraceRecordEntry} ORM entity, in both directions.
 *
 * <p>Receipts, chain registrations and producer keys need no such mapping: {@code TraceStore}
 * exposes their jans-orm entities ({@code TraceReceiptEntry}, {@code TraceChainEntry},
 * {@code TraceProducerKeyEntry}) directly, unlike {@code StoredTraceRecord} — the two shapes
 * differ enough (structured identities vs. flat hashed keys, defensive copies on read) that a
 * dedicated value type still earns its keep for records.
 *
 * <p>{@code TraceRecordEntry} only carries <em>hashed</em> keys for the producer chain
 * identity and token references ({@code jansTraceChainKey}, {@code jansTraceTokenKeys}) — it
 * never stores {@code producer_instance_id}, {@code producer_chain_id} or the raw token references
 * as separate attributes.
 * On the read path ({@link #toStoredTraceRecord(TraceRecordEntry)}) those raw values are recovered
 * by re-parsing the verbatim {@code jansTraceAssertion} text, consistent with the design's
 * "assertion is the source of truth, retrieval re-parses it" principle. This is a
 * deliberate re-derivation, not a schema round trip: the stored hashes remain the only thing that
 * is ever compared for equality/lookup.
 *
 * @author Yuriy Movchan
 */
final class TraceEntityMapper {

	private static final ObjectMapper ASSERTION_MAPPER = new ObjectMapper();

	private TraceEntityMapper() {
	}

	// -- records ------------------------------------------------------------------------------

	static TraceRecordEntry toEntity(StoredTraceRecord stored, String dn, TraceKeys traceKeys) {
		RecordIdentity identity = stored.getIdentity();
		ChainPosition chainPosition = stored.getChainPosition();
		ReceiptEntry receipt = stored.getReceipt();
		IngestionFlags flags = stored.getFlags();

		TraceRecordEntry entity = new TraceRecordEntry();
		entity.setDn(dn);
		entity.setId(traceKeys.recordKey(identity));
		entity.setDomainId(identity.getDomainId());
		entity.setProducerId(identity.getProducerId());
		entity.setRecordId(identity.getRecordId());
		entity.setEventKind(stored.getEventKind());
		entity.setSignedAt(stored.getSignedAt());
		entity.setExecKey(traceKeys.executionKey(stored.getExecution()));
		entity.setExecId(stored.getExecution().getTraceExecutionId());
		entity.setExecAuthority(stored.getExecution().getExecutionAuthority());
		entity.setChainKey(traceKeys.chainKey(chainPosition.getChainIdentity()));
		entity.setChainPosKey(traceKeys.chainPositionKey(chainPosition));
		entity.setSeqNum(chainPosition.getSequenceNumber());
		entity.setPrevRecordHash(stored.getPrevRecordHash());
		entity.setTokenKeys(stored.getTokenRefs().stream()
				.map(tokenRef -> traceKeys.tokenKey(identity.getDomainId(), tokenRef)).collect(Collectors.toList()));
		entity.setAssertion(stored.getAssertionRaw());
		entity.setContentDigest(stored.getContentDigest());
		entity.setVerification(toVerificationPojo(stored.getVerification()));
		entity.setReceiptSeq(receipt.getReceiptSequence());
		entity.setReceivedAt(new Date(receipt.getReceivedAtMs()));
		entity.setReceivedAtMs(receipt.getReceivedAtMs());
		entity.setPrevReceiptHash(receipt.getPrevReceiptHash());
		entity.setReceiptHash(receipt.getReceiptHash());
		entity.setCoverageGap(flags.isCoverageGap());
		entity.setChainLinkFailure(flags.isChainLinkFailure());
		entity.setEquivocation(flags.isEquivocation());
		entity.setLate(flags.isLate());
		entity.setPolicyMismatch(flags.isPolicyMismatch());
		entity.setReservedMember(flags.isReservedMember());
		entity.setNodeId(stored.getNodeId());
		PolicyRef policy = stored.getPolicy();
		if (policy != null) {
			entity.setPolicyStoreId(policy.getPolicyStoreId());
			entity.setPolicyStoreVersion(policy.getPolicyStoreVersion());
			entity.setBundleHash(policy.getBundleHash());
		}
		entity.setVersion(stored.getVersion());
		entity.setCreationDate(new Date(stored.getCreatedAtMs()));
		return entity;
	}

	static StoredTraceRecord toStoredTraceRecord(TraceRecordEntry entity) {
		String key = entity.getId();
		RecordIdentity identity = new RecordIdentity(entity.getDomainId(), entity.getProducerId(),
				entity.getRecordId());
		ExecutionIdentity execution = new ExecutionIdentity(entity.getDomainId(), entity.getExecAuthority(),
				entity.getExecId());

		JsonNode assertionRoot = parseAssertion(entity.getAssertion(), key);
		ChainIdentity chainIdentity = extractChainIdentity(assertionRoot, entity.getDomainId(),
				entity.getProducerId(), key);
		ChainPosition chainPosition = new ChainPosition(chainIdentity, nvl(entity.getSeqNum()));
		List<TokenRef> tokenRefs = extractTokenRefs(assertionRoot);

		VerificationResult verification = toVerificationResult(entity.getVerification());
		ReceiptEntry receipt = new ReceiptEntry(nvl(entity.getReceiptSeq()), nvl(entity.getReceivedAtMs()),
				entity.getPrevReceiptHash(), entity.getReceiptHash());
		IngestionFlags flags = new IngestionFlags(nvl(entity.getCoverageGap()), nvl(entity.getChainLinkFailure()),
				nvl(entity.getEquivocation()), nvl(entity.getLate()), nvl(entity.getPolicyMismatch()));
		flags.setReservedMember(nvl(entity.getReservedMember()));

		long createdAtMs = entity.getCreationDate() == null ? 0L : entity.getCreationDate().getTime();

		return new StoredTraceRecord(identity, entity.getAssertion(), entity.getContentDigest(), verification,
				receipt, flags, execution, chainPosition, entity.getPrevRecordHash(), tokenRefs, entity.getEventKind(),
				nvl(entity.getSignedAt()), entity.getNodeId(), createdAtMs, nvl(entity.getVersion()),
				toPolicyRef(entity));
	}

	private static PolicyRef toPolicyRef(TraceRecordEntry entity) {
		if (entity.getPolicyStoreId() == null || entity.getPolicyStoreVersion() == null
				|| entity.getBundleHash() == null) {
			return null;
		}
		return new PolicyRef(entity.getPolicyStoreId(), entity.getPolicyStoreVersion(), entity.getBundleHash());
	}

	private static TraceVerification toVerificationPojo(VerificationResult verification) {
		TraceVerification pojo = new TraceVerification();
		pojo.setSignatureValid(verification.isSignatureValid());
		pojo.setKeyId(verification.getKeyId());
		pojo.setVerifiedAtMs(verification.getVerifiedAtMs());
		pojo.setAlgorithm(verification.getAlgorithm());
		pojo.setKeyThumbprint(verification.getKeyThumbprint());
		pojo.setKeyAuthorized(verification.getKeyAuthorized());
		return pojo;
	}

	private static VerificationResult toVerificationResult(TraceVerification pojo) {
		return new VerificationResult(pojo.isSignatureValid(), pojo.getKeyId(), pojo.getVerifiedAtMs(),
				pojo.getAlgorithm(), pojo.getKeyThumbprint(), pojo.getKeyAuthorized());
	}

	private static JsonNode parseAssertion(String assertionText, String key) {
		try {
			return ASSERTION_MAPPER.readTree(assertionText);
		} catch (IOException ex) {
			throw new TraceStorageException("assertion_parse_failed",
					"Failed to re-parse the stored assertion for record " + key, ex);
		}
	}

	private static ChainIdentity extractChainIdentity(JsonNode assertionRoot, String domainId, String producerId,
			String key) {
		JsonNode producerChain = assertionRoot.path("producer_chain");
		String producerInstanceId = requireText(producerChain, "producer_instance_id", key);
		String producerChainId = requireText(producerChain, "producer_chain_id", key);
		return new ChainIdentity(domainId, producerId, producerInstanceId, producerChainId);
	}

	private static String requireText(JsonNode parent, String field, String key) {
		JsonNode node = parent.path(field);
		if (!node.isTextual()) {
			throw new TraceStorageException("assertion_field_missing", "Stored assertion for record " + key
					+ " is missing required field producer_chain." + field);
		}
		return node.textValue();
	}

	private static List<TokenRef> extractTokenRefs(JsonNode assertionRoot) {
		JsonNode tokensNode = assertionRoot.path("trace").path("event").path("tokens");
		if (!tokensNode.isArray()) {
			return Collections.emptyList();
		}
		List<TokenRef> result = new ArrayList<>(tokensNode.size());
		for (JsonNode entry : tokensNode) {
			String issuer = entry.path("issuer").isTextual() ? entry.path("issuer").textValue() : null;
			String tokenType = entry.path("token_type").isTextual() ? entry.path("token_type").textValue() : null;
			String jti = entry.path("jti").isTextual() ? entry.path("jti").textValue() : null;
			String fingerprint = entry.path("fingerprint").isTextual() ? entry.path("fingerprint").textValue() : null;
			result.add(new TokenRef(issuer, tokenType, jti, fingerprint));
		}
		return Collections.unmodifiableList(result);
	}

	// -- null-safety helpers --------------------------------------------------------------------

	private static long nvl(Long value) {
		return value == null ? 0L : value;
	}

	private static boolean nvl(Boolean value) {
		return value != null && value;
	}

}
