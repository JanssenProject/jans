/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds and signs TRACE MVP assertions (design §7) for {@code TraceEndToEndTest}. Each builder
 * method returns the fully signed wire JSON, ready to POST to {@code /api/v1/audit/trace}.
 */
public final class TraceAssertions {

	public static final String EAT_PROFILE = "tag:jans.io,2026:trace-v1";

	public static final String ZERO_HASH = "sha256:" + "0".repeat(64);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TraceAssertions() {
	}

	public static final class AuthorizationDecision {

		private String producer;
		private String kid;
		private String recordId;
		private long signedAt;
		private String traceExecutionId;
		private String executionAuthority;
		private String producerInstanceId;
		private String producerChainId;
		private long sequenceNumber = 1L;
		private String prevRecordHash = ZERO_HASH;

		public AuthorizationDecision producer(String producer) {
			this.producer = producer;
			return this;
		}

		public AuthorizationDecision kid(String kid) {
			this.kid = kid;
			return this;
		}

		public AuthorizationDecision recordId(String recordId) {
			this.recordId = recordId;
			return this;
		}

		public AuthorizationDecision signedAt(long signedAt) {
			this.signedAt = signedAt;
			return this;
		}

		public AuthorizationDecision traceExecutionId(String traceExecutionId) {
			this.traceExecutionId = traceExecutionId;
			return this;
		}

		public AuthorizationDecision executionAuthority(String executionAuthority) {
			this.executionAuthority = executionAuthority;
			return this;
		}

		public AuthorizationDecision producerInstanceId(String producerInstanceId) {
			this.producerInstanceId = producerInstanceId;
			return this;
		}

		public AuthorizationDecision producerChainId(String producerChainId) {
			this.producerChainId = producerChainId;
			return this;
		}

		public AuthorizationDecision sequenceNumber(long sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
			return this;
		}

		public AuthorizationDecision prevRecordHash(String prevRecordHash) {
			this.prevRecordHash = prevRecordHash;
			return this;
		}

		/** @return the unsigned tree (no {@code signature} field yet) */
		public ObjectNode unsignedTree() {
			ObjectNode root = MAPPER.createObjectNode();
			root.put("producer", producer);
			root.put("kid", kid);
			root.put("record_id", recordId);

			ObjectNode trace = root.putObject("trace");
			trace.put("eat_profile", EAT_PROFILE);
			trace.put("event_kind", "AUTHORIZATION_DECISION");
			trace.put("signed_at", signedAt);
			trace.put("trace_execution_id", traceExecutionId);
			trace.put("execution_authority", executionAuthority);

			ObjectNode event = trace.putObject("event");
			event.put("outcome", "ALLOW");
			ArrayNode capabilityIds = event.putArray("capability_ids");
			ObjectNode capability = capabilityIds.addObject();
			capability.put("capability_id", "invoke:payment-authorization");
			capability.put("outcome", "ALLOW");

			ObjectNode policy = trace.putObject("policy");
			policy.put("policy_store_id", "https://example.org/policy-stores/payments");
			policy.put("policy_store_version", "1.0.0");
			policy.put("policy_language", "cedar");
			policy.put("policy_language_version", "4.4.0");
			policy.put("bundle_hash", ZERO_HASH);

			ObjectNode runtime = trace.putObject("runtime");
			runtime.put("pdp_id", "trace-end-to-end-test");

			ObjectNode chain = root.putObject("producer_chain");
			chain.put("producer_id", producer);
			chain.put("producer_instance_id", producerInstanceId);
			chain.put("producer_chain_id", producerChainId);
			chain.put("sequence_number", sequenceNumber);
			chain.put("prev_record_hash", prevRecordHash);

			root.putArray("parent_record_ids");
			return root;
		}

		/** @return the signed wire JSON, ready to submit */
		public String sign(TraceSigningKey key) {
			return TraceAssertions.sign(unsignedTree(), key);
		}
	}

	public static final class CapabilityInvoked {

		private String producer;
		private String kid;
		private String recordId;
		private long signedAt;
		private String traceExecutionId;
		private String executionAuthority;
		private String producerInstanceId;
		private String producerChainId;
		private long sequenceNumber = 1L;
		private String prevRecordHash = ZERO_HASH;
		private String parentProducerId;
		private String parentRecordId;

		public CapabilityInvoked producer(String producer) {
			this.producer = producer;
			return this;
		}

		public CapabilityInvoked kid(String kid) {
			this.kid = kid;
			return this;
		}

		public CapabilityInvoked recordId(String recordId) {
			this.recordId = recordId;
			return this;
		}

		public CapabilityInvoked signedAt(long signedAt) {
			this.signedAt = signedAt;
			return this;
		}

		public CapabilityInvoked traceExecutionId(String traceExecutionId) {
			this.traceExecutionId = traceExecutionId;
			return this;
		}

		public CapabilityInvoked executionAuthority(String executionAuthority) {
			this.executionAuthority = executionAuthority;
			return this;
		}

		public CapabilityInvoked producerInstanceId(String producerInstanceId) {
			this.producerInstanceId = producerInstanceId;
			return this;
		}

		public CapabilityInvoked producerChainId(String producerChainId) {
			this.producerChainId = producerChainId;
			return this;
		}

		public CapabilityInvoked sequenceNumber(long sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
			return this;
		}

		public CapabilityInvoked prevRecordHash(String prevRecordHash) {
			this.prevRecordHash = prevRecordHash;
			return this;
		}

		public CapabilityInvoked parent(String parentProducerId, String parentRecordId) {
			this.parentProducerId = parentProducerId;
			this.parentRecordId = parentRecordId;
			return this;
		}

		public ObjectNode unsignedTree() {
			ObjectNode root = MAPPER.createObjectNode();
			root.put("producer", producer);
			root.put("kid", kid);
			root.put("record_id", recordId);

			ObjectNode trace = root.putObject("trace");
			trace.put("eat_profile", EAT_PROFILE);
			trace.put("event_kind", "CAPABILITY_INVOKED");
			trace.put("signed_at", signedAt);
			trace.put("trace_execution_id", traceExecutionId);
			trace.put("execution_authority", executionAuthority);

			ObjectNode event = trace.putObject("event");
			event.put("outcome", "INVOKED");
			event.put("enforcement_point_id", "trace-end-to-end-test-gateway");
			ArrayNode capabilityIds = event.putArray("capability_ids");
			ObjectNode capability = capabilityIds.addObject();
			capability.put("capability_id", "invoke:payment-authorization");
			capability.put("outcome", "ALLOW");

			ObjectNode chain = root.putObject("producer_chain");
			chain.put("producer_id", producer);
			chain.put("producer_instance_id", producerInstanceId);
			chain.put("producer_chain_id", producerChainId);
			chain.put("sequence_number", sequenceNumber);
			chain.put("prev_record_hash", prevRecordHash);

			ArrayNode parents = root.putArray("parent_record_ids");
			if (parentProducerId != null) {
				ObjectNode parent = parents.addObject();
				parent.put("producer_id", parentProducerId);
				parent.put("record_id", parentRecordId);
				parent.put("relationship_type", "authorized_by");
			}
			return root;
		}

		public String sign(TraceSigningKey key) {
			return TraceAssertions.sign(unsignedTree(), key);
		}
	}

	/** @return {@code tree} serialized to JSON after adding a {@code signature} field signed with {@code key} */
	public static String sign(ObjectNode tree, TraceSigningKey key) {
		byte[] signatureInput = TraceJcs.canonicalBytes(tree);
		String signature = key.signToBase64Url(signatureInput);
		ObjectNode signed = tree.deepCopy();
		signed.put("signature", signature);
		try {
			return MAPPER.writeValueAsString(signed);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to serialize signed TRACE assertion", ex);
		}
	}

	/** @return {@code content_digest} (design §10/D-13): sha256 of the JCS form including {@code signature} */
	public static String contentDigest(String signedJson) {
		try {
			JsonNode node = MAPPER.readTree(signedJson);
			return TraceJcs.sha256Digest(node);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to parse signed TRACE assertion", ex);
		}
	}

}
