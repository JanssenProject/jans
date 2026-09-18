package io.jans.configapi.filters;
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.model.configuration.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test client for CorsFilter. Exercises the filter directly (mocked servlet
 * request/response + mocked CorsConfiguration) rather than over the wire,
 * so each policy decision can be pinned down deterministically:
 *
 *  - explicit allow-list is mandatory (null/empty list => deny, never allow-all)
 *  - a matched origin is echoed back verbatim (not "*", not an unvalidated reflection)
 *  - wildcard ("*") configuration and Access-Control-Allow-Credentials are mutually exclusive
 *  - Access-Control-Allow-Credentials is only ever sent alongside a validated, non-wildcard origin
 *  - an origin that fails the allow-list check gets no CORS headers at all
 */
@ExtendWith(MockitoExtension.class)
class CorsFilterTest {

    private static final String ORIGIN_HEADER = "Origin";
    private static final String ALLOWED_ORIGIN = "https://admin.example.org";
    private static final String DISALLOWED_ORIGIN = "https://evil.example.net";

    @Mock
    private CorsConfiguration corsConfiguration;

    @Mock
    private Logger logger;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorsFilter corsFilter;

    @BeforeEach
    void setUp() throws Exception {
        corsFilter = new CorsFilter();
        inject(corsFilter, "log", logger);
        inject(corsFilter, "corsConfiguration", corsConfiguration);

        lenient().when(corsConfiguration.isEnabled()).thenReturn(true);
        lenient().when(request.getHeader(ORIGIN_HEADER)).thenReturn(ALLOWED_ORIGIN);
        lenient().when(request.getMethod()).thenReturn("GET");
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = CorsFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // --- Explicit allow-list is mandatory -----------------------------------------------------

    @Test
    void nullAllowList_setsNoAccessControlAllowOriginHeader() throws IOException, ServletException {
        when(corsConfiguration.getAllowedOrigins()).thenReturn(null);

        corsFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN), anyString());
        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void emptyAllowList_setsNoAccessControlAllowOriginHeader() throws IOException, ServletException {
        when(corsConfiguration.getAllowedOrigins()).thenReturn(Collections.emptyList());

        corsFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN), anyString());
        verify(filterChain).doFilter(request, response);
    }

    // --- Wildcard and credentials are mutually exclusive --------------------------------------

    @Test
    void wildcardConfigured_setsWildcardOrigin_neverSetsCredentials() throws IOException, ServletException {
        List<String> allowed = Arrays.asList("*");
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        // Even if the server-wide flag is on, wildcard responses must never carry credentials.
        lenient().when(corsConfiguration.isSupportsCredentials()).thenReturn(true);

        corsFilter.doFilter(request, response, filterChain);

        verify(response).addHeader(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS), anyString());
    }

    // --- Matched origin: echoed verbatim, credentials gated correctly --------------------------

    @Test
    void allowedOriginWithCredentialsSupport_echoesOriginAndSetsCredentialsTrue()
            throws IOException, ServletException {
        List<String> allowed = Arrays.asList(ALLOWED_ORIGIN);
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        when(corsConfiguration.isOriginAllowed(ALLOWED_ORIGIN)).thenReturn(true);
        when(corsConfiguration.isSupportsCredentials()).thenReturn(true);

        corsFilter.doFilter(request, response, filterChain);

        verify(response).addHeader(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
        verify(response).addHeader(CorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void allowedOriginWithoutCredentialsSupport_echoesOriginOnly() throws IOException, ServletException {
        List<String> allowed = Arrays.asList(ALLOWED_ORIGIN);
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        when(corsConfiguration.isOriginAllowed(ALLOWED_ORIGIN)).thenReturn(true);
        when(corsConfiguration.isSupportsCredentials()).thenReturn(false);

        corsFilter.doFilter(request, response, filterChain);

        verify(response).addHeader(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS), anyString());
    }

    // --- Unmatched origin: no CORS headers at all ----------------------------------------------

    @Test
    void disallowedOrigin_setsNoCorsHeaders_evenWhenCredentialsSupportEnabled()
            throws IOException, ServletException {
        when(request.getHeader(ORIGIN_HEADER)).thenReturn(DISALLOWED_ORIGIN);
        List<String> allowed = Arrays.asList(ALLOWED_ORIGIN);
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        when(corsConfiguration.isOriginAllowed(DISALLOWED_ORIGIN)).thenReturn(false);
        // This must have no effect: an unmatched origin never gets credentials, regardless
        // of the server-wide flag - this is the exact regression the fix closes.
        lenient().when(corsConfiguration.isSupportsCredentials()).thenReturn(true);

        corsFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN), anyString());
        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS), anyString());
        verify(filterChain).doFilter(request, response);
    }

    // --- Disabled / blank-origin short-circuits --------------------------------------------------

    @Test
    void corsDisabled_skipsProcessing_stillChains() throws IOException, ServletException {
        when(corsConfiguration.isEnabled()).thenReturn(false);

        corsFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blankOrigin_skipsProcessing_stillChains() throws IOException, ServletException {
        when(request.getHeader(ORIGIN_HEADER)).thenReturn(null);

        corsFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN), anyString());
        verify(filterChain).doFilter(request, response);
    }

    // --- Vary header --------------------------------------------------------------------------

    @Test
    void allowedOrigin_setsVaryOriginHeader() throws IOException, ServletException {
        List<String> allowed = Arrays.asList(ALLOWED_ORIGIN);
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        when(corsConfiguration.isOriginAllowed(ALLOWED_ORIGIN)).thenReturn(true);
        lenient().when(corsConfiguration.isSupportsCredentials()).thenReturn(false);

        corsFilter.doFilter(request, response, filterChain);

        verify(response).addHeader(CorsFilter.VARY, "Origin");
    }

    // --- Preflight (OPTIONS) ---------------------------------------------------------------------

    @Test
    void preflightOptions_allowedOrigin_setsMaxAge_doesNotChainFurther() throws IOException, ServletException {
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getHeader(CorsFilter.ACCESS_CONTROL_REQUEST_METHOD)).thenReturn("POST");
        List<String> allowed = Arrays.asList(ALLOWED_ORIGIN);
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        when(corsConfiguration.isOriginAllowed(ALLOWED_ORIGIN)).thenReturn(true);
        lenient().when(corsConfiguration.isSupportsCredentials()).thenReturn(false);
        when(corsConfiguration.getPreflightMaxAge()).thenReturn(3600);

        corsFilter.doFilter(request, response, filterChain);

        verify(response).addHeader(CorsFilter.ACCESS_CONTROL_MAX_AGE, "3600");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void preflightOptions_disallowedOrigin_setsNoHeaders_doesNotChain() throws IOException, ServletException {
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getHeader(ORIGIN_HEADER)).thenReturn(DISALLOWED_ORIGIN);
        List<String> allowed = Arrays.asList(ALLOWED_ORIGIN);
        when(corsConfiguration.getAllowedOrigins()).thenReturn(allowed);
        when(corsConfiguration.isOriginAllowed(DISALLOWED_ORIGIN)).thenReturn(false);

        corsFilter.doFilter(request, response, filterChain);

        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN), anyString());
        verify(response, never()).addHeader(eq(CorsFilter.ACCESS_CONTROL_MAX_AGE), anyString());
        // Same as the non-preflight case: no CORS headers, request still allowed to
        // continue server-side (the browser is what enforces the block).
        verify(filterChain).doFilter(request, response);
    }
}