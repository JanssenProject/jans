package io.jans.configapi.test.filter;
/*
 * Janssen Project software is available under the MIT License (2008).
 * See http://opensource.org/licenses/MIT for full text.
 * Copyright (c) 2020, Janssen Project
 */



import io.jans.configapi.filters.SecurityResponseHeadersFilter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Unit test for SecurityResponseHeadersFilter.
 *
 * Exercises the filter directly against mocked JAX-RS context objects, so it
 * needs no running server, no network, and no test.properties - just the
 * filter class itself plus TestNG + Mockito on the test classpath.
 *
 *
 * Suggested test-scope dependency (if not already present in the module's
 * pom.xml):
 *   org.mockito:mockito-core
 *
 * 
 */
public class SecurityResponseHeadersFilterTest {

    private static final String HEADER_CSP = "Content-Security-Policy";
    private static final String HEADER_XFO = "X-Frame-Options";
    private static final String HEADER_XCTO = "X-Content-Type-Options";
    private static final String HEADER_REFERRER = "Referrer-Policy";

    private static final String EXPECTED_CSP = "default-src 'none'; frame-ancestors 'none'";
    private static final String EXPECTED_XFO = "DENY";
    private static final String EXPECTED_XCTO = "nosniff";
    private static final String EXPECTED_REFERRER = "no-referrer";

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    @Mock
    private UriInfo uriInfo;

    private SecurityResponseHeadersFilter filter;
    private MultivaluedMap<String, Object> headers;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        filter = new SecurityResponseHeadersFilter();
        headers = new MultivaluedHashMap<>();

        Mockito.when(responseContext.getHeaders()).thenReturn(headers);
        Mockito.when(requestContext.getUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPath()).thenReturn("/jans-auth-server/config");
    }

    @Test
    public void addsAllFourSecurityHeadersWhenNonePresent() throws IOException {
        filter.filter(requestContext, responseContext);

        assertSingleHeader(HEADER_CSP, EXPECTED_CSP);
        assertSingleHeader(HEADER_XFO, EXPECTED_XFO);
        assertSingleHeader(HEADER_XCTO, EXPECTED_XCTO);
        assertSingleHeader(HEADER_REFERRER, EXPECTED_REFERRER);
    }

    @Test
    public void cspValueContainsStrictFrameAncestorsDirective() throws IOException {
        filter.filter(requestContext, responseContext);

        String csp = (String) headers.getFirst(HEADER_CSP);
        Assert.assertNotNull(csp);
        Assert.assertTrue(csp.contains("frame-ancestors 'none'"),
                "CSP should contain a strict frame-ancestors directive, got: " + csp);
    }

    @Test
    public void xFrameOptionsAndCspFrameAncestorsStayInLockstep() throws IOException {
        // Both controls exist to enforce the same "do not frame this" policy;
        // this test pins that relationship so a future edit can't silently
        // loosen one while leaving the other strict.
        filter.filter(requestContext, responseContext);

        String csp = (String) headers.getFirst(HEADER_CSP);
        String xfo = (String) headers.getFirst(HEADER_XFO);

        boolean cspDenies = csp != null && csp.contains("frame-ancestors 'none'");
        boolean xfoDenies = "DENY".equals(xfo);

        Assert.assertEquals(cspDenies, xfoDenies,
                "CSP frame-ancestors and X-Frame-Options must express the same framing policy");
    }

    @Test
    public void doesNotOverwriteHeaderAlreadySetByAnUpstreamFilterOrResource() throws IOException {
        // A more specific, deliberately-set value (e.g. a resource that has a
        // legitimate reason to allow framing from a specific origin) must
        // survive - this filter should only fill gaps, never clobber.
        headers.putSingle(HEADER_XFO, "SAMEORIGIN");

        filter.filter(requestContext, responseContext);

        Assert.assertEquals(headers.getFirst(HEADER_XFO), "SAMEORIGIN",
                "Filter must not overwrite a header a resource/filter already set");
        // The other three headers, which were never set, should still be added.
        assertSingleHeader(HEADER_CSP, EXPECTED_CSP);
        assertSingleHeader(HEADER_XCTO, EXPECTED_XCTO);
        assertSingleHeader(HEADER_REFERRER, EXPECTED_REFERRER);
    }

    @Test
    public void appliesHeadersRegardlessOfResponseStatus() throws IOException {
        // The filter must not be conditional on status - it should decorate
        // error responses (401/403/500 from exception mappers) exactly like
        // a 200. Simulate a 401 by having the mock report that status; the
        // filter implementation doesn't branch on it, so headers should still
        // be applied identically.
        Mockito.when(responseContext.getStatus()).thenReturn(401);

        filter.filter(requestContext, responseContext);

        assertSingleHeader(HEADER_CSP, EXPECTED_CSP);
        assertSingleHeader(HEADER_XFO, EXPECTED_XFO);
        assertSingleHeader(HEADER_XCTO, EXPECTED_XCTO);
        assertSingleHeader(HEADER_REFERRER, EXPECTED_REFERRER);
    }

    @Test
    public void doesNotThrowWhenUriInfoIsUnavailable() throws IOException {
        // Defensive: some request contexts (e.g. certain error paths) may not
        // have a fully populated UriInfo. The filter only uses it for a debug
        // log line, so a null UriInfo must not break header application.
        Mockito.when(requestContext.getUriInfo()).thenReturn(null);

        filter.filter(requestContext, responseContext);

        assertSingleHeader(HEADER_CSP, EXPECTED_CSP);
    }

    private void assertSingleHeader(String name, String expectedValue) {
        Assert.assertTrue(headers.containsKey(name), "Missing header: " + name);
        Assert.assertEquals(headers.get(name).size(), 1,
                "Expected exactly one value for header " + name);
        Assert.assertEquals(headers.getFirst(name), expectedValue,
                "Unexpected value for header " + name);
    }
}