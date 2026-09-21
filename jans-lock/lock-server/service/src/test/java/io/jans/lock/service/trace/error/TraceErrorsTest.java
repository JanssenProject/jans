/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.crypto.TraceCryptoException;
import io.jans.lock.service.trace.parse.TraceValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link TraceErrors#toWebApplicationException} — the mapping from each TRACE runtime
 * exception to the {@link TraceErrorResponseType} it carries (design decision D-10).
 */
class TraceErrorsTest {

	private ErrorResponseFactory newFactory() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getErrorReasonEnabled()).thenReturn(true);
		return new ErrorResponseFactory(null, appConfiguration);
	}

	private String errorOf(WebApplicationException ex) {
		return new JSONObject((String) ex.getResponse().getEntity()).getString("error");
	}

	@Test
	void testToWebApplicationException_traceValidationException_usesItsErrorId() {
		TraceValidationException ex = new TraceValidationException(TraceErrorResponseType.INVALID_ASSERTION, "missing:trace.signed_at");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("invalid_assertion", errorOf(wae));
	}

	@Test
	void testToWebApplicationException_traceConflictException_recordConflict() {
		TraceConflictException ex = new TraceConflictException(TraceErrorResponseType.RECORD_CONFLICT, "digest_mismatch");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.CONFLICT.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("record_conflict", errorOf(wae));
	}

	@Test
	void testToWebApplicationException_traceConflictException_ambiguousIdentifier() {
		TraceConflictException ex = new TraceConflictException(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, "multiple_producers");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.CONFLICT.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("ambiguous_identifier", errorOf(wae));
	}

	@Test
	void testToWebApplicationException_traceStorageException_alwaysMapsToStorageFailure() {
		TraceStorageException ex = new TraceStorageException("receipt_allocation_retry_exhausted", "gave up after retries");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("storage_failure", errorOf(wae));
	}

	@Test
	void testToWebApplicationException_traceCryptoException_providerUnavailable_mapsToCryptoUnavailable() {
		TraceCryptoException ex = new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, "no Ed25519 provider");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("crypto_unavailable", errorOf(wae));
	}

	@Test
	void testToWebApplicationException_traceCryptoException_jwkFormat_mapsToInvalidKey() {
		TraceCryptoException ex = new TraceCryptoException(TraceCryptoException.REASON_JWK_KTY, "kty must be OKP");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("invalid_key", errorOf(wae));
	}

	@Test
	void testToWebApplicationException_unrecognizedRuntimeException_mapsToGenericServerError() {
		RuntimeException ex = new IllegalStateException("unexpected");

		WebApplicationException wae = TraceErrors.toWebApplicationException(ex, newFactory());

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), wae.getResponse().getStatus());
		assertEquals("unknown_error", errorOf(wae));
	}

}
