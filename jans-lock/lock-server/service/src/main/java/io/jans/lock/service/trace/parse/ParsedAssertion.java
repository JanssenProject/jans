/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.canon.CanonicalHashes;
import io.jans.lock.service.trace.canon.JcsCanonicalizer;

/**
 * The bounded-parsed form of an incoming TRACE assertion: the exact received bytes decoded as
 * UTF-8 ({@link #getRawText()}) and the parsed tree ({@link #getRoot()}), plus the typed common
 * fields (design §7.1) filled in by {@link CommonAssertionValidator} once it accepts the
 * assertion.
 *
 * <p>{@code rawText} and {@code root} are fixed at construction and never change. The typed
 * accessors are {@code null} (or, for {@code long} fields, meaningless) until
 * {@link CommonAssertionValidator#validate(ParsedAssertion)} returns without throwing; callers
 * outside the {@code parse} package can only read them, never set them.
 *
 * @author Yuriy Movchan
 */
public final class ParsedAssertion {

	private final String rawText;

	private final ObjectNode root;

	private String producer;

	private String kid;

	private String recordId;

	private String eatProfile;

	private String eventKind;

	private long signedAt;

	private String traceExecutionId;

	private String executionAuthority;

	private ProducerChain producerChain;

	private List<ParentRecordId> parentRecordIds = Collections.emptyList();

	private String signature;

	private JsonNode subject;

	private byte[] signatureInput;

	private String contentDigest;

	ParsedAssertion(String rawText, ObjectNode root) {
		this.rawText = rawText;
		this.root = root;
	}

	/**
	 * @return the exact request body, decoded as UTF-8; this is what gets stored as the record's
	 *         {@code assertion} text
	 */
	public String getRawText() {
		return rawText;
	}

	/**
	 * @return the parsed JSON tree of the whole assertion, including {@code signature}
	 */
	public ObjectNode getRoot() {
		return root;
	}

	public String getProducer() {
		return producer;
	}

	void setProducer(String producer) {
		this.producer = producer;
	}

	public String getKid() {
		return kid;
	}

	void setKid(String kid) {
		this.kid = kid;
	}

	public String getRecordId() {
		return recordId;
	}

	void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public String getEatProfile() {
		return eatProfile;
	}

	void setEatProfile(String eatProfile) {
		this.eatProfile = eatProfile;
	}

	public String getEventKind() {
		return eventKind;
	}

	void setEventKind(String eventKind) {
		this.eventKind = eventKind;
	}

	public long getSignedAt() {
		return signedAt;
	}

	void setSignedAt(long signedAt) {
		this.signedAt = signedAt;
	}

	public String getTraceExecutionId() {
		return traceExecutionId;
	}

	void setTraceExecutionId(String traceExecutionId) {
		this.traceExecutionId = traceExecutionId;
	}

	public String getExecutionAuthority() {
		return executionAuthority;
	}

	void setExecutionAuthority(String executionAuthority) {
		this.executionAuthority = executionAuthority;
	}

	public ProducerChain getProducerChain() {
		return producerChain;
	}

	void setProducerChain(ProducerChain producerChain) {
		this.producerChain = producerChain;
	}

	/**
	 * @return the (possibly empty) list of causal references; never {@code null}
	 */
	public List<ParentRecordId> getParentRecordIds() {
		return parentRecordIds;
	}

	void setParentRecordIds(List<ParentRecordId> parentRecordIds) {
		this.parentRecordIds = parentRecordIds == null ? Collections.emptyList()
				: Collections.unmodifiableList(parentRecordIds);
	}

	/**
	 * @return the signature, still base64url-encoded (not yet decoded to raw bytes)
	 */
	public String getSignature() {
		return signature;
	}

	void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * @return {@code trace.subject}, or {@code null} if the assertion carries none
	 */
	public JsonNode getSubject() {
		return subject;
	}

	void setSubject(JsonNode subject) {
		this.subject = subject;
	}

	/**
	 * @return {@code JCS(assertion without the top-level "signature" field)}, UTF-8 encoded
	 *         (design D-13); computed on first use and cached
	 */
	public byte[] signatureInput() {
		if (signatureInput == null) {
			signatureInput = JcsCanonicalizer
					.canonicalizeToUtf8(JcsCanonicalizer.withoutField(root, TraceConstants.SIGNATURE_FIELD));
		}
		return signatureInput;
	}

	/**
	 * @return {@code "sha256:" + lowercaseHex(SHA-256(UTF-8(JCS(full assertion))))} (design D-13);
	 *         computed on first use and cached
	 */
	public String contentDigest() {
		if (contentDigest == null) {
			contentDigest = CanonicalHashes.sha256PrefixedOfJcs(root);
		}
		return contentDigest;
	}

	/**
	 * {@code producer_chain} (design §7.1, §8): identifies a producer instance's local chain and
	 * position within it.
	 */
	public static final class ProducerChain {

		private final String producerId;

		private final String producerInstanceId;

		private final String producerChainId;

		private final long sequenceNumber;

		private final String prevRecordHash;

		public ProducerChain(String producerId, String producerInstanceId, String producerChainId,
				long sequenceNumber, String prevRecordHash) {
			this.producerId = producerId;
			this.producerInstanceId = producerInstanceId;
			this.producerChainId = producerChainId;
			this.sequenceNumber = sequenceNumber;
			this.prevRecordHash = prevRecordHash;
		}

		public String getProducerId() {
			return producerId;
		}

		public String getProducerInstanceId() {
			return producerInstanceId;
		}

		public String getProducerChainId() {
			return producerChainId;
		}

		public long getSequenceNumber() {
			return sequenceNumber;
		}

		public String getPrevRecordHash() {
			return prevRecordHash;
		}

	}

	/**
	 * One entry of {@code parent_record_ids} (design §7.1): a typed causal reference to a record
	 * in the same evidence domain.
	 */
	public static final class ParentRecordId {

		private final String producerId;

		private final String recordId;

		private final String relationshipType;

		public ParentRecordId(String producerId, String recordId, String relationshipType) {
			this.producerId = producerId;
			this.recordId = recordId;
			this.relationshipType = relationshipType;
		}

		public String getProducerId() {
			return producerId;
		}

		public String getRecordId() {
			return recordId;
		}

		public String getRelationshipType() {
			return relationshipType;
		}

	}

}
