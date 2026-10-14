/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.testkit;

import java.security.PrivateKey;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Task 22 consolidated test kit entry point for building signed TRACE assertions: a thin fluent
 * wrapper over {@link SignedAssertionFactory} (tasks 16/19) that additionally assigns a fresh
 * {@code record_id} (UUID v4) by default, so a test only calls {@link #recordId(String)} when the
 * identifier itself matters. Delegates every other operation to {@link SignedAssertionFactory}
 * rather than duplicating it.
 */
public final class TraceRecordBuilder {

	public static final String FIXTURE_AUTHORIZATION_DECISION = SignedAssertionFactory.FIXTURE_AUTHORIZATION_DECISION;

	public static final String FIXTURE_CAPABILITY_INVOKED = SignedAssertionFactory.FIXTURE_CAPABILITY_INVOKED;

	public static final String FIXTURE_RUNTIME_EFFECT = SignedAssertionFactory.FIXTURE_RUNTIME_EFFECT;

	private final SignedAssertionFactory delegate;

	private TraceRecordBuilder(SignedAssertionFactory delegate) {
		this.delegate = delegate;
	}

	// -- construction -----------------------------------------------------------------------------

	/**
	 * @param fixtureName a file name under {@code /trace/assertions/}, e.g.
	 *                    {@link #FIXTURE_AUTHORIZATION_DECISION}
	 * @return a builder seeded from the fixture with a freshly generated {@code record_id}
	 */
	public static TraceRecordBuilder fromFixture(String fixtureName) {
		return new TraceRecordBuilder(SignedAssertionFactory.fromFixture(fixtureName)).recordId(freshRecordId());
	}

	/**
	 * @param json any JSON object text; used to re-sign an assertion produced earlier. The
	 *             {@code record_id} already in {@code json} is kept as-is.
	 */
	public static TraceRecordBuilder fromJson(String json) {
		return new TraceRecordBuilder(SignedAssertionFactory.fromJson(json));
	}

	public static TraceRecordBuilder authorizationDecision() {
		return fromFixture(FIXTURE_AUTHORIZATION_DECISION);
	}

	public static TraceRecordBuilder capabilityInvoked() {
		return fromFixture(FIXTURE_CAPABILITY_INVOKED);
	}

	public static TraceRecordBuilder runtimeEffect() {
		return fromFixture(FIXTURE_RUNTIME_EFFECT);
	}

	/** @return a fresh UUID v4 string, usable directly as {@code record_id} or a ULID-free substitute */
	public static String freshRecordId() {
		return UUID.randomUUID().toString();
	}

	// -- field overrides ----------------------------------------------------------------------------

	public TraceRecordBuilder withField(JsonNode value, String... path) {
		delegate.withField(value, path);
		return this;
	}

	public TraceRecordBuilder withField(String value, String... path) {
		delegate.withField(value, path);
		return this;
	}

	public TraceRecordBuilder withField(long value, String... path) {
		delegate.withField(value, path);
		return this;
	}

	public TraceRecordBuilder withoutField(String... path) {
		delegate.withoutField(path);
		return this;
	}

	public TraceRecordBuilder producer(String producer) {
		delegate.producer(producer);
		return this;
	}

	public TraceRecordBuilder kid(String kid) {
		delegate.kid(kid);
		return this;
	}

	public TraceRecordBuilder recordId(String recordId) {
		delegate.recordId(recordId);
		return this;
	}

	public TraceRecordBuilder signedAt(long signedAtSeconds) {
		delegate.signedAt(signedAtSeconds);
		return this;
	}

	public TraceRecordBuilder traceExecutionId(String traceExecutionId) {
		delegate.traceExecutionId(traceExecutionId);
		return this;
	}

	public TraceRecordBuilder executionAuthority(String executionAuthority) {
		delegate.executionAuthority(executionAuthority);
		return this;
	}

	public TraceRecordBuilder producerInstanceId(String producerInstanceId) {
		delegate.producerInstanceId(producerInstanceId);
		return this;
	}

	public TraceRecordBuilder producerChainId(String producerChainId) {
		delegate.producerChainId(producerChainId);
		return this;
	}

	public TraceRecordBuilder sequenceNumber(long sequenceNumber) {
		delegate.sequenceNumber(sequenceNumber);
		return this;
	}

	public TraceRecordBuilder prevRecordHash(String prevRecordHash) {
		delegate.prevRecordHash(prevRecordHash);
		return this;
	}

	/** Convenience for genesis records: {@code sequence_number = 1} and the zero-hash sentinel. */
	public TraceRecordBuilder genesis() {
		delegate.genesis();
		return this;
	}

	/** @return a deep copy of the current (unsigned or previously signed) tree */
	public ObjectNode tree() {
		return delegate.tree();
	}

	// -- signing ------------------------------------------------------------------------------------

	public String sign(PrivateKey privateKey) {
		return delegate.sign(privateKey);
	}

	public String signWrongScope(PrivateKey privateKey) {
		return delegate.signWrongScope(privateKey);
	}

	public String unsignedJson() {
		return delegate.unsignedJson();
	}

	// -- post-signing transformations -----------------------------------------------------------------

	public static String tamperAfterSigning(String json, JsonNode value, String... path) {
		return SignedAssertionFactory.tamperAfterSigning(json, value, path);
	}

	public static String tamperAfterSigning(String json, String value, String... path) {
		return SignedAssertionFactory.tamperAfterSigning(json, value, path);
	}

	/**
	 * @return the same JSON value serialized with every object's members in reverse order and with
	 *         pretty-printing whitespace; JCS-equivalent to {@code json}, textually different
	 */
	public static String reorderAndPrettyPrint(String json) {
		return SignedAssertionFactory.reorderAndPrettyPrint(json);
	}

}
