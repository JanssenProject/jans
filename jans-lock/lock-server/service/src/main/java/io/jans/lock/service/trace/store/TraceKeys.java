/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.jans.lock.service.trace.canon.CanonicalHashes;
import io.jans.lock.service.trace.canon.JcsCanonicalizer;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.TokenRef;

/**
 * The D-6 composite-key function and the four DN builders (design decision D-6, D-7):
 *
 * <pre>
 * key(type, c1, ..., cn) = lowercaseHex( SHA-256( UTF-8( JCS( [type, c1, ..., cn] ) ) ) )
 * </pre>
 *
 * {@code type} and every component are serialized as JSON strings — including the sequence number
 * and receipt sequence, which are converted to their decimal string form before being placed in
 * the array — so JCS array serialization (which preserves element order and never merges adjacent
 * elements) makes every distinct component vector produce a distinct key; there is no separator
 * for a crafted component value to collide with.
 *
 * @author Yuriy Movchan
 */
public final class TraceKeys {

	private TraceKeys() {
	}

	/** Key type {@code rec}: {@code (domain, producer_id, record_id)}. */
	public static String recordKey(RecordIdentity id) {
		Objects.requireNonNull(id, "id");
		return key("rec", id.getDomainId(), id.getProducerId(), id.getRecordId());
	}

	/** Key type {@code exec}: {@code (domain, execution_authority, trace_execution_id)}. */
	public static String executionKey(ExecutionIdentity id) {
		Objects.requireNonNull(id, "id");
		return key("exec", id.getDomainId(), id.getExecutionAuthority(), id.getTraceExecutionId());
	}

	/** Key type {@code chain}: {@code (domain, producer_id, producer_instance_id, producer_chain_id)}. */
	public static String chainKey(ChainIdentity id) {
		Objects.requireNonNull(id, "id");
		return key("chain", id.getDomainId(), id.getProducerId(), id.getProducerInstanceId(),
				id.getProducerChainId());
	}

	/**
	 * Key type {@code pos}: {@code (domain, producer_id, producer_instance_id, producer_chain_id,
	 * sequence_number)}.
	 */
	public static String chainPositionKey(ChainPosition position) {
		Objects.requireNonNull(position, "position");
		ChainIdentity chain = position.getChainIdentity();
		return key("pos", chain.getDomainId(), chain.getProducerId(), chain.getProducerInstanceId(),
				chain.getProducerChainId(), Long.toString(position.getSequenceNumber()));
	}

	/** Key type {@code cap}: {@code (domain, capability_id)}. */
	public static String capabilityKey(String domainId, String capabilityId) {
		return key("cap", domainId, capabilityId);
	}

	/**
	 * Key type {@code tok}: {@code (domain, issuer, "jti", jti)} when the reference carries a
	 * {@code jti}, otherwise {@code (domain, issuer, "fp", fingerprint)}. The literal discriminator
	 * ({@code "jti"}/{@code "fp"}) is itself a key component, so the two branches never collide;
	 * {@link TokenRef#getTokenType()} is not part of the key.
	 */
	public static String tokenKey(String domainId, TokenRef token) {
		Objects.requireNonNull(token, "token");
		if (token.getJti() != null) {
			return key("tok", domainId, token.getIssuer(), "jti", token.getJti());
		}
		return key("tok", domainId, token.getIssuer(), "fp", token.getFingerprint());
	}

	/** Key type {@code pkey}: {@code (domain, producer_id, kid)}. */
	public static String producerKeyKey(String domainId, String producerId, String kid) {
		return key("pkey", domainId, producerId, kid);
	}

	/** Key type {@code rcpt}: {@code (domain, receipt_sequence)}. */
	public static String receiptKey(String domainId, long receiptSequence) {
		return key("rcpt", domainId, Long.toString(receiptSequence));
	}

	/** {@code jansId=<key>,ou=records,<baseDn>}. */
	public static String recordDn(String baseDn, String key) {
		return dn(baseDn, "records", key);
	}

	/** {@code jansId=<key>,ou=receipts,<baseDn>}. */
	public static String receiptDn(String baseDn, String key) {
		return dn(baseDn, "receipts", key);
	}

	/** {@code jansId=<key>,ou=chains,<baseDn>}. */
	public static String chainDn(String baseDn, String key) {
		return dn(baseDn, "chains", key);
	}

	/** {@code jansId=<key>,ou=keys,<baseDn>}. */
	public static String producerKeyDn(String baseDn, String key) {
		return dn(baseDn, "keys", key);
	}

	private static String dn(String baseDn, String ou, String key) {
		Objects.requireNonNull(baseDn, "baseDn");
		Objects.requireNonNull(key, "key");
		return "jansId=" + key + ",ou=" + ou + "," + baseDn;
	}

	private static String key(String type, String... components) {
		Objects.requireNonNull(type, "type");
		ArrayNode array = JsonNodeFactory.instance.arrayNode(components.length + 1);
		array.add(type);
		for (String component : components) {
			array.add(Objects.requireNonNull(component, "key component"));
		}
		String hash = CanonicalHashes.sha256Prefixed(JcsCanonicalizer.canonicalizeToUtf8(array));
		return hash.substring(CanonicalHashes.SHA256_PREFIX.length());
	}

}
