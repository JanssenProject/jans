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
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceKeys;

/**
 * Pure mapping between the task 12 {@link StoredTraceRecord} value type and the task 03
 * {@link TraceRecordEntry} ORM entity, in both directions (design decisions D-6, D-7).
 *
 * <p>Receipts, chain registrations and producer keys need no such mapping: {@code TraceStore}
 * exposes their jans-orm entities ({@code TraceReceiptEntry}, {@code TraceChainEntry},
 * {@code TraceProducerKeyEntry}) directly, unlike {@code StoredTraceRecord} — the two shapes
 * differ enough (structured identities vs. flat hashed keys, defensive copies on read) that a
 * dedicated value type still earns its keep for records (deviation from task 12's original
 * value-type-for-every-row design, approved 2026; see the TRACE MVP task notes).
 *
 * <p>{@code TraceRecordEntry} (T-1) only carries <em>hashed</em> keys for the producer chain
 * identity, capability ids and token references ({@code jansTraceChainKey}, {@code jansTraceCapKeys},
 * {@code jansTraceTokenKeys}) — it never stores {@code producer_instance_id}, {@code
 * producer_chain_id}, the raw capability ids or the raw token references as separate attributes.
 * On the read path ({@link #toStoredTraceRecord(TraceRecordEntry)}) those raw values are recovered
 * by re-parsing the verbatim {@code jansTraceAssertion} text, consistent with the design's
 * "assertion is the source of truth, retrieval re-parses it" principle (design D-13). This is a
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

	static TraceRecordEntry toEntity(StoredTraceRecord record, String dn) {
		RecordIdentity identity = record.getIdentity();
		ChainPosition chainPosition = record.getChainPosition();
		ReceiptEntry receipt = record.getReceipt();
		IngestionFlags flags = record.getFlags();

		TraceRecordEntry entity = new TraceRecordEntry();
		entity.setDn(dn);
		entity.setId(TraceKeys.recordKey(identity));
		entity.setDomainId(identity.getDomainId());
		entity.setProducerId(identity.getProducerId());
		entity.setRecordId(identity.getRecordId());
		entity.setEventKind(record.getEventKind());
		entity.setSignedAt(record.getSignedAt());
		entity.setExecKey(TraceKeys.executionKey(record.getExecution()));
		entity.setExecId(record.getExecution().getTraceExecutionId());
		entity.setExecAuthority(record.getExecution().getExecutionAuthority());
		entity.setChainKey(TraceKeys.chainKey(chainPosition.getChainIdentity()));
		entity.setChainPosKey(TraceKeys.chainPositionKey(chainPosition));
		entity.setSeqNum(chainPosition.getSequenceNumber());
		entity.setPrevRecordHash(record.getPrevRecordHash());
		entity.setCapKeys(record.getCapabilityIds().stream()
				.map(capabilityId -> TraceKeys.capabilityKey(identity.getDomainId(), capabilityId))
				.collect(Collectors.toList()));
		entity.setTokenKeys(record.getTokenRefs().stream()
				.map(tokenRef -> TraceKeys.tokenKey(identity.getDomainId(), tokenRef)).collect(Collectors.toList()));
		entity.setAssertion(record.getAssertionRaw());
		entity.setContentDigest(record.getContentDigest());
		entity.setVerification(toVerificationPojo(record.getVerification()));
		entity.setReceiptSeq(receipt.getReceiptSequence());
		entity.setReceivedAt(new Date(receipt.getReceivedAtMs()));
		entity.setReceivedAtMs(receipt.getReceivedAtMs());
		entity.setPrevReceiptHash(receipt.getPrevReceiptHash());
		entity.setReceiptHash(receipt.getReceiptHash());
		entity.setCoverageGap(flags.isCoverageGap());
		entity.setChainLinkFailure(flags.isChainLinkFailure());
		entity.setEquivocation(flags.isEquivocation());
		entity.setLate(flags.isLate());
		entity.setNodeId(record.getNodeId());
		entity.setCreationDate(new Date(record.getCreatedAtMs()));
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
		List<String> capabilityIds = extractCapabilityIds(assertionRoot);
		List<TokenRef> tokenRefs = extractTokenRefs(assertionRoot);

		VerificationResult verification = toVerificationResult(entity.getVerification());
		ReceiptEntry receipt = new ReceiptEntry(nvl(entity.getReceiptSeq()), nvl(entity.getReceivedAtMs()),
				entity.getPrevReceiptHash(), entity.getReceiptHash());
		IngestionFlags flags = new IngestionFlags(nvl(entity.getCoverageGap()), nvl(entity.getChainLinkFailure()),
				nvl(entity.getEquivocation()), nvl(entity.getLate()));

		long createdAtMs = entity.getCreationDate() == null ? 0L : entity.getCreationDate().getTime();

		return new StoredTraceRecord(identity, entity.getAssertion(), entity.getContentDigest(), verification,
				receipt, flags, execution, chainPosition, entity.getPrevRecordHash(), capabilityIds, tokenRefs,
				entity.getEventKind(), nvl(entity.getSignedAt()), entity.getNodeId(), createdAtMs);
	}

	private static TraceVerification toVerificationPojo(VerificationResult verification) {
		TraceVerification pojo = new TraceVerification();
		pojo.setSignatureValid(verification.isSignatureValid());
		pojo.setKeyId(verification.getKeyId());
		pojo.setVerifiedAtMs(verification.getVerifiedAtMs());
		pojo.setAlgorithm(verification.getAlgorithm());
		return pojo;
	}

	private static VerificationResult toVerificationResult(TraceVerification pojo) {
		return new VerificationResult(pojo.isSignatureValid(), pojo.getKeyId(), pojo.getVerifiedAtMs(),
				pojo.getAlgorithm());
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

	private static List<String> extractCapabilityIds(JsonNode assertionRoot) {
		JsonNode capabilityIdsNode = assertionRoot.path("trace").path("event").path("capability_ids");
		if (!capabilityIdsNode.isArray()) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>(capabilityIdsNode.size());
		for (JsonNode entry : capabilityIdsNode) {
			JsonNode idNode = entry.path("capability_id");
			if (idNode.isTextual()) {
				result.add(idNode.textValue());
			}
		}
		return Collections.unmodifiableList(result);
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
