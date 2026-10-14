/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.testkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.canon.JcsCanonicalizer;
import io.jans.lock.service.trace.crypto.Ed25519TestKeys;
import io.jans.lock.service.trace.crypto.StrictBase64Url;

/**
 * Test-only builder of signed TRACE assertions. Starts from one of the fixture templates under
 * {@code src/test/resources/trace/assertions/}, lets a test override or remove any field by path,
 * signs the result with a test private key over {@code JCS(assertion without "signature")}
 * (design D-13) and returns the JSON text a client would POST.
 *
 * <p>Shared by the verification (task 16), ingestion (task 19) and acceptance (task 22) tests.
 *
 * <p>Path segments navigate object members; a segment that parses as an integer indexes an array,
 * e.g. {@code "trace", "event", "tokens", "0", "jti"}.
 */
public final class SignedAssertionFactory {

	/** Task 09's design §7 {@code AUTHORIZATION_DECISION} example. */
	public static final String FIXTURE_AUTHORIZATION_DECISION = "authorization_decision_valid.json";

	/** Task 09's design §7 {@code CAPABILITY_INVOKED} example. */
	public static final String FIXTURE_CAPABILITY_INVOKED = "capability_invoked_valid.json";

	/** Task 09's design §7 {@code RUNTIME_EFFECT} example. */
	public static final String FIXTURE_RUNTIME_EFFECT = "runtime_effect_valid.json";

	private static final String FIXTURE_ROOT = "/trace/assertions/";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final ObjectNode root;

	private SignedAssertionFactory(ObjectNode root) {
		this.root = root;
	}

	// -- construction -----------------------------------------------------------------------------

	/**
	 * @param fixtureName a file name under {@code /trace/assertions/}, e.g.
	 *                    {@link #FIXTURE_AUTHORIZATION_DECISION}
	 */
	public static SignedAssertionFactory fromFixture(String fixtureName) {
		try (InputStream is = SignedAssertionFactory.class.getResourceAsStream(FIXTURE_ROOT + fixtureName)) {
			if (is == null) {
				throw new IllegalArgumentException("Fixture not found: " + FIXTURE_ROOT + fixtureName);
			}
			return new SignedAssertionFactory((ObjectNode) MAPPER.readTree(is));
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * @param json any JSON object text; used to re-sign an assertion produced earlier
	 */
	public static SignedAssertionFactory fromJson(String json) {
		return new SignedAssertionFactory((ObjectNode) parse(json));
	}

	public static SignedAssertionFactory authorizationDecision() {
		return fromFixture(FIXTURE_AUTHORIZATION_DECISION);
	}

	public static SignedAssertionFactory capabilityInvoked() {
		return fromFixture(FIXTURE_CAPABILITY_INVOKED);
	}

	public static SignedAssertionFactory runtimeEffect() {
		return fromFixture(FIXTURE_RUNTIME_EFFECT);
	}

	// -- field overrides ----------------------------------------------------------------------------

	/**
	 * Sets (or replaces) the value at {@code path}. Intermediate objects must already exist.
	 */
	public SignedAssertionFactory withField(JsonNode value, String... path) {
		set(root, value, path);
		return this;
	}

	public SignedAssertionFactory withField(String value, String... path) {
		return withField(TextNode.valueOf(value), path);
	}

	public SignedAssertionFactory withField(long value, String... path) {
		return withField(MAPPER.getNodeFactory().numberNode(value), path);
	}

	/**
	 * Removes the member (or array element) at {@code path}; a no-op if it is absent.
	 */
	public SignedAssertionFactory withoutField(String... path) {
		remove(root, path);
		return this;
	}

	/**
	 * Sets {@code producer} and {@code producer_chain.producer_id} together so the §7.1 equality
	 * rule keeps holding.
	 */
	public SignedAssertionFactory producer(String producer) {
		withField(producer, "producer");
		return withField(producer, "producer_chain", "producer_id");
	}

	public SignedAssertionFactory kid(String kid) {
		return withField(kid, "kid");
	}

	public SignedAssertionFactory recordId(String recordId) {
		return withField(recordId, "record_id");
	}

	public SignedAssertionFactory signedAt(long signedAtSeconds) {
		return withField(signedAtSeconds, "trace", "signed_at");
	}

	public SignedAssertionFactory traceExecutionId(String traceExecutionId) {
		return withField(traceExecutionId, "trace", "trace_execution_id");
	}

	public SignedAssertionFactory executionAuthority(String executionAuthority) {
		return withField(executionAuthority, "trace", "execution_authority");
	}

	public SignedAssertionFactory producerInstanceId(String producerInstanceId) {
		return withField(producerInstanceId, "producer_chain", "producer_instance_id");
	}

	public SignedAssertionFactory producerChainId(String producerChainId) {
		return withField(producerChainId, "producer_chain", "producer_chain_id");
	}

	public SignedAssertionFactory sequenceNumber(long sequenceNumber) {
		return withField(sequenceNumber, "producer_chain", "sequence_number");
	}

	public SignedAssertionFactory prevRecordHash(String prevRecordHash) {
		return withField(prevRecordHash, "producer_chain", "prev_record_hash");
	}

	/**
	 * Convenience for genesis records: {@code sequence_number = 1} and the zero-hash sentinel
	 * (decision D-5).
	 */
	public SignedAssertionFactory genesis() {
		sequenceNumber(1);
		return prevRecordHash(TraceConstants.ZERO_HASH);
	}

	/**
	 * @return a deep copy of the current (unsigned or previously signed) tree
	 */
	public ObjectNode tree() {
		return root.deepCopy();
	}

	// -- signing ------------------------------------------------------------------------------------

	/**
	 * Signs {@code JCS(assertion without "signature")} (design D-13), stores the base64url
	 * signature in the tree and returns the compact JSON text.
	 */
	public String sign(PrivateKey privateKey) {
		byte[] input = JcsCanonicalizer
				.canonicalizeToUtf8(JcsCanonicalizer.withoutField(root, TraceConstants.SIGNATURE_FIELD));
		return putSignature(Ed25519TestKeys.sign(privateKey, input));
	}

	/**
	 * Signs over the <em>wrong</em> scope — {@code JCS(full assertion)} with the current (template
	 * or placeholder) {@code signature} member still present — and stores that signature. A
	 * verifier that strips {@code signature} before verifying can never accept the result.
	 */
	public String signWrongScope(PrivateKey privateKey) {
		if (!root.has(TraceConstants.SIGNATURE_FIELD)) {
			root.put(TraceConstants.SIGNATURE_FIELD, StrictBase64Url.encode(new byte[64]));
		}
		byte[] input = JcsCanonicalizer.canonicalizeToUtf8(root);
		return putSignature(Ed25519TestKeys.sign(privateKey, input));
	}

	/**
	 * @return the compact JSON text of the tree as-is (with whatever {@code signature} it carries)
	 */
	public String unsignedJson() {
		return toJson(root);
	}

	private String putSignature(byte[] signature) {
		root.put(TraceConstants.SIGNATURE_FIELD, StrictBase64Url.encode(signature));
		return toJson(root);
	}

	// -- post-signing transformations -----------------------------------------------------------------

	/**
	 * Changes a field <em>after</em> signing without re-signing: parses {@code json}, sets the value
	 * at {@code path} and re-serializes. The signature no longer covers the content.
	 */
	public static String tamperAfterSigning(String json, JsonNode value, String... path) {
		ObjectNode tree = (ObjectNode) parse(json);
		set(tree, value, path);
		return toJson(tree);
	}

	public static String tamperAfterSigning(String json, String value, String... path) {
		return tamperAfterSigning(json, TextNode.valueOf(value), path);
	}

	/**
	 * @return the same JSON value serialized with every object's members in reverse order and with
	 *         pretty-printing whitespace; JCS-equivalent to {@code json}, textually different
	 */
	public static String reorderAndPrettyPrint(String json) {
		JsonNode reordered = reverseMemberOrder(parse(json));
		try {
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(reordered);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	// -- helpers ------------------------------------------------------------------------------------

	private static JsonNode parse(String json) {
		try {
			JsonNode node = MAPPER.readTree(json);
			if (!node.isObject()) {
				throw new IllegalArgumentException("Assertion JSON must be an object");
			}
			return node;
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static String toJson(JsonNode node) {
		try {
			return MAPPER.writeValueAsString(node);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static JsonNode reverseMemberOrder(JsonNode node) {
		if (node.isObject()) {
			List<Map.Entry<String, JsonNode>> members = new ArrayList<>();
			for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
				members.add(it.next());
			}
			Collections.reverse(members);
			ObjectNode copy = MAPPER.createObjectNode();
			for (Map.Entry<String, JsonNode> member : members) {
				copy.set(member.getKey(), reverseMemberOrder(member.getValue()));
			}
			return copy;
		}
		if (node.isArray()) {
			ArrayNode copy = MAPPER.createArrayNode();
			for (JsonNode child : node) {
				copy.add(reverseMemberOrder(child));
			}
			return copy;
		}
		return node;
	}

	private static JsonNode child(JsonNode node, String segment) {
		return node.isArray() ? node.get(Integer.parseInt(segment)) : node.get(segment);
	}

	private static JsonNode at(JsonNode root, String... path) {
		JsonNode node = root;
		for (String segment : path) {
			if (node == null) {
				throw new IllegalArgumentException("No node at path " + Arrays.toString(path));
			}
			node = child(node, segment);
		}
		return node;
	}

	private static void set(ObjectNode root, JsonNode value, String... path) {
		if (path.length == 0) {
			throw new IllegalArgumentException("path must not be empty");
		}
		JsonNode parent = at(root, Arrays.copyOf(path, path.length - 1));
		if (parent == null) {
			throw new IllegalArgumentException("No parent node at path " + Arrays.toString(path));
		}
		String last = path[path.length - 1];
		if (parent.isArray()) {
			((ArrayNode) parent).set(Integer.parseInt(last), value);
		} else {
			((ObjectNode) parent).set(last, value);
		}
	}

	private static void remove(ObjectNode root, String... path) {
		if (path.length == 0) {
			throw new IllegalArgumentException("path must not be empty");
		}
		JsonNode parent = at(root, Arrays.copyOf(path, path.length - 1));
		if (parent == null) {
			return;
		}
		String last = path[path.length - 1];
		if (parent.isArray()) {
			((ArrayNode) parent).remove(Integer.parseInt(last));
		} else {
			((ObjectNode) parent).remove(last);
		}
	}

}
