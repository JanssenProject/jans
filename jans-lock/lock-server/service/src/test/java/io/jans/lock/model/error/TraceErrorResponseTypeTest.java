/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response.Status;

/**
 * Tests for {@link TraceErrorResponseType} against design decision D-10's id/status table.
 */
class TraceErrorResponseTypeTest {

	@Test
	void testValues_everyIdIsUniqueAndNonBlank() {
		Set<String> ids = new HashSet<>();
		for (TraceErrorResponseType type : TraceErrorResponseType.values()) {
			assertNotNull(type.getParameter());
			assertNotNull(type.httpStatus());
			assertEquals(type.getParameter(), type.toString());
			assertTrueUnique(ids, type.getParameter());
		}
		assertEquals(21, TraceErrorResponseType.values().length);
	}

	private static void assertTrueUnique(Set<String> ids, String id) {
		if (!ids.add(id)) {
			throw new AssertionError("Duplicate TraceErrorResponseType id: " + id);
		}
	}

	@Test
	void testHttpStatus_matchesDesignDecisionD10Table() {
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.INVALID_REQUEST.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.INVALID_ASSERTION.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.UNSUPPORTED_PROFILE.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.DOMAIN_FIELD_FORBIDDEN.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.PRODUCER_MISMATCH.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.UNKNOWN_PRODUCER_KEY.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.KEY_NOT_VALID.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.INVALID_SIGNATURE.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.CHAIN_NOT_REGISTERED.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.INVALID_GENESIS.httpStatus());
		assertSame(Status.FORBIDDEN, TraceErrorResponseType.CLIENT_NOT_BOUND.httpStatus());
		assertSame(Status.FORBIDDEN, TraceErrorResponseType.PRODUCER_NOT_ALLOWED.httpStatus());
		assertSame(Status.NOT_FOUND, TraceErrorResponseType.RECORD_NOT_FOUND.httpStatus());
		assertSame(Status.NOT_FOUND, TraceErrorResponseType.EXECUTION_NOT_FOUND.httpStatus());
		assertSame(Status.CONFLICT, TraceErrorResponseType.RECORD_CONFLICT.httpStatus());
		assertSame(Status.CONFLICT, TraceErrorResponseType.AMBIGUOUS_IDENTIFIER.httpStatus());
		assertSame(Status.CONFLICT, TraceErrorResponseType.KEY_ALREADY_EXISTS.httpStatus());
		assertSame(Status.CONFLICT, TraceErrorResponseType.CHAIN_ALREADY_EXISTS.httpStatus());
		assertSame(Status.BAD_REQUEST, TraceErrorResponseType.INVALID_KEY.httpStatus());
		assertSame(Status.INTERNAL_SERVER_ERROR, TraceErrorResponseType.STORAGE_FAILURE.httpStatus());
		assertSame(Status.INTERNAL_SERVER_ERROR, TraceErrorResponseType.CRYPTO_UNAVAILABLE.httpStatus());
	}

	@Test
	void testFromString_knownId_returnsMatchingType() {
		assertSame(TraceErrorResponseType.RECORD_CONFLICT, TraceErrorResponseType.fromString("record_conflict"));
		assertSame(TraceErrorResponseType.CRYPTO_UNAVAILABLE, TraceErrorResponseType.fromString("crypto_unavailable"));
	}

	@Test
	void testFromString_unknownOrNullId_returnsNull() {
		assertNull(TraceErrorResponseType.fromString("no_such_error"));
		assertNull(TraceErrorResponseType.fromString(null));
	}

}
