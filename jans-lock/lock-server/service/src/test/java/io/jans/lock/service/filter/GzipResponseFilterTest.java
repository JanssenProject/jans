/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Tests for {@link GzipResponseFilter}: gzip is requested only for clients that accept it.
 */
class GzipResponseFilterTest {

	private final GzipResponseFilter filter = new GzipResponseFilter();

	private MultivaluedMap<String, Object> run(String acceptEncoding, boolean hasEntity, String existingEncoding) throws Exception {
		ContainerRequestContext request = mock(ContainerRequestContext.class);
		when(request.getHeaderString(HttpHeaders.ACCEPT_ENCODING)).thenReturn(acceptEncoding);

		MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
		if (existingEncoding != null) {
			headers.putSingle(HttpHeaders.CONTENT_ENCODING, existingEncoding);
		}
		ContainerResponseContext response = mock(ContainerResponseContext.class);
		when(response.hasEntity()).thenReturn(hasEntity);
		when(response.getHeaders()).thenReturn(headers);

		filter.filter(request, response);
		return headers;
	}

	@ParameterizedTest
	@ValueSource(strings = { "gzip", "gzip, deflate", "deflate, GZIP", "gzip;q=0.5", "br, gzip ; q=1.0" })
	void gzipAccepted_setsContentEncoding(String acceptEncoding) throws Exception {
		MultivaluedMap<String, Object> headers = run(acceptEncoding, true, null);

		assertEquals("gzip", headers.getFirst(HttpHeaders.CONTENT_ENCODING));
		assertEquals(HttpHeaders.ACCEPT_ENCODING, headers.getFirst(HttpHeaders.VARY));
	}

	@ParameterizedTest
	@ValueSource(strings = { "identity", "deflate", "gzip;q=0", "gzip;q=abc", "" })
	void gzipNotAccepted_leavesPlain(String acceptEncoding) throws Exception {
		MultivaluedMap<String, Object> headers = run(acceptEncoding, true, null);

		assertNull(headers.getFirst(HttpHeaders.CONTENT_ENCODING));
		assertEquals(HttpHeaders.ACCEPT_ENCODING, headers.getFirst(HttpHeaders.VARY));
	}

	@Test
	void noAcceptEncodingHeader_leavesPlain() throws Exception {
		MultivaluedMap<String, Object> headers = run(null, true, null);

		assertNull(headers.getFirst(HttpHeaders.CONTENT_ENCODING));
	}

	@Test
	void noEntity_leavesResponseUntouched() throws Exception {
		MultivaluedMap<String, Object> headers = run("gzip", false, null);

		assertTrue(headers.isEmpty());
	}

	@Test
	void existingContentEncoding_keptAsIs() throws Exception {
		MultivaluedMap<String, Object> headers = run("gzip", true, "br");

		assertEquals("br", headers.getFirst(HttpHeaders.CONTENT_ENCODING));
		assertFalse(headers.containsKey(HttpHeaders.VARY));
	}

}
