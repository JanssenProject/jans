/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.config.AppConfiguration;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link ErrorResponseFactory}'s TRACE error family support (design decision D-10, task
 * 10). The fixture at {@code src/test/resources/trace/errors.json} is a copy of
 * {@code jans-linux-setup/jans_setup/templates/jans-lock/errors.json} (the setup template that
 * seeds {@code Conf.jansConfErrors}); keep the two in sync when the TRACE error catalog changes.
 */
class ErrorResponseFactoryTraceTest {

	private static final String FIXTURE_PATH = "/trace/errors.json";

	private io.jans.lock.model.config.ErrorMessages messages;

	@BeforeEach
	void setUp() throws IOException {
		try (InputStream in = getClass().getResourceAsStream(FIXTURE_PATH)) {
			assertNotNull(in, "Missing test fixture " + FIXTURE_PATH);
			messages = new ObjectMapper().readValue(in, io.jans.lock.model.config.ErrorMessages.class);
		}
	}

	private ErrorResponseFactory newFactory(boolean errorReasonEnabled) {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getErrorReasonEnabled()).thenReturn(errorReasonEnabled);
		return new ErrorResponseFactory(messages, appConfiguration);
	}

	@Test
	void testFixture_containsExactlyTheEnumIds() {
		assertNotNull(messages.getTrace());
		assertEquals(TraceErrorResponseType.values().length, messages.getTrace().size());
		for (TraceErrorResponseType type : TraceErrorResponseType.values()) {
			boolean found = messages.getTrace().stream().anyMatch(m -> type.getParameter().equals(m.getId()));
			assertTrue(found, "Fixture is missing id: " + type.getParameter());
		}
	}

	@Test
	void testTraceException_everyType_yieldsD10StatusAndMatchingErrorBody() throws IOException {
		ErrorResponseFactory factory = newFactory(true);

		for (TraceErrorResponseType type : TraceErrorResponseType.values()) {
			WebApplicationException ex = factory.traceException(type, "x");

			Response response = ex.getResponse();
			assertEquals(type.httpStatus().getStatusCode(), response.getStatus(),
					"Unexpected status for " + type.getParameter());

			JSONObject body = new JSONObject((String) response.getEntity());
			assertEquals(type.getParameter(), body.getString("error"));
			assertTrue(body.has("error_description"));
			assertEquals("x", body.getString("reason"));
		}
	}

	@Test
	void testTraceException_errorReasonDisabled_reasonIsEmpty() {
		ErrorResponseFactory factory = newFactory(false);

		WebApplicationException ex = factory.traceException(TraceErrorResponseType.INVALID_ASSERTION, "some-detail");

		JSONObject body = new JSONObject((String) ex.getResponse().getEntity());
		assertEquals("invalid_assertion", body.getString("error"));
		assertFalse(body.has("reason"), "reason must not be present when errorReasonEnabled=false");
	}

	@Test
	void testTraceException_withCause_stillProducesStructuredBody() {
		ErrorResponseFactory factory = newFactory(true);

		WebApplicationException ex = factory.traceException(TraceErrorResponseType.STORAGE_FAILURE, "persistence_failure",
				new RuntimeException("boom"));

		assertEquals(500, ex.getResponse().getStatus());
		JSONObject body = new JSONObject((String) ex.getResponse().getEntity());
		assertEquals("storage_failure", body.getString("error"));
		assertEquals("persistence_failure", body.getString("reason"));
	}

	@Test
	void testGetErrorResponse_missingTraceList_fallsBackToEnumIdInstead() {
		io.jans.lock.model.config.ErrorMessages noTraceSection = new io.jans.lock.model.config.ErrorMessages();
		noTraceSection.setCommon(messages.getCommon());
		noTraceSection.setStat(messages.getStat());
		// trace intentionally left null: simulates an older deployment's persisted errors config.

		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getErrorReasonEnabled()).thenReturn(true);
		ErrorResponseFactory factory = new ErrorResponseFactory(noTraceSection, appConfiguration);

		WebApplicationException ex = factory.traceException(TraceErrorResponseType.RECORD_CONFLICT, "x");

		assertEquals(409, ex.getResponse().getStatus());
		JSONObject body = new JSONObject((String) ex.getResponse().getEntity());
		assertEquals("record_conflict", body.getString("error"));
		assertEquals("record_conflict", body.getString("error_description"));
	}

}
