/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.error.TraceValidationException;

/**
 * Tests for {@link ForwardingPolicy}: design decision D-3's wildcard and exact-match rules.
 */
class ForwardingPolicyTest {

	private static TraceRequestContext ctx(String... allowedProducerIds) {
		return new TraceRequestContext("client-1", "domain-a", Arrays.asList(allowedProducerIds), 1L, "node-1");
	}

	@Test
	void testCheck_WildcardAllowlist_AllowsAnyProducer() {
		assertDoesNotThrow(() -> ForwardingPolicy.check(ctx("*"), "any-producer/9.9.9"));
	}

	@Test
	void testCheck_ExactMatch_Allows() {
		assertDoesNotThrow(() -> ForwardingPolicy.check(ctx("a/1.0.0"), "a/1.0.0"));
	}

	@Test
	void testCheck_DifferentVersion_Rejected() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> ForwardingPolicy.check(ctx("a/1.0.0"), "a/1.0.1"));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
		assertEquals(ForwardingPolicy.REASON_PRODUCER_NOT_ALLOWED, ex.getReason());
	}

	@Test
	void testCheck_UnlistedProducer_Rejected() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> ForwardingPolicy.check(ctx("a/1.0.0", "b/2.0.0"), "c/1.0.0"));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
	}

	@Test
	void testCheck_EmptyAllowlist_Rejected() {
		TraceRequestContext context = new TraceRequestContext("client-1", "domain-a", Collections.emptyList(), 1L,
				"node-1");

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> ForwardingPolicy.check(context, "any/1.0.0"));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
	}

}
