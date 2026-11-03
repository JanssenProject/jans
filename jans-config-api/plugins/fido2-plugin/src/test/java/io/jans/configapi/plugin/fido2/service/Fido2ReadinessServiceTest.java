/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.plugin.fido2.service.Fido2ReadinessService.Outcome;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.plugin.fido2.util.Fido2Util;
import io.jans.configapi.security.service.OpenIdService;
import io.jans.model.net.HttpServiceResponse;

import jakarta.ws.rs.WebApplicationException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Fido2ReadinessServiceTest {

    private static final String ISSUER = "https://jans.example.org";
    private static final String BASE = ISSUER + "/jans-fido2/restv1/rp/";

    private Fido2Util fido2Util;
    private OpenIdService openIdService;
    private Fido2ReadinessService service;

    @BeforeMethod
    public void setUp() {
        fido2Util = mock(Fido2Util.class);
        when(fido2Util.getIssuer()).thenReturn(ISSUER);
        service = new Fido2ReadinessService();
        service.fido2Util = fido2Util;
        openIdService = mock(OpenIdService.class);
        service.openIdService = openIdService;
        introspect("inum-1234", "client-1");
        service.log = mock(Logger.class);
    }

    private void introspect(String inum, String clientId) {
        introspect(inum, null, clientId);
    }

    private void introspect(String inum, String username, String clientId) {
        IntrospectionResponse introspection = mock(IntrospectionResponse.class);
        when(introspection.getClientId()).thenReturn(clientId);
        when(introspection.getUsername()).thenReturn(username);
        if (inum != null) {
            when(introspection.getAuthorizationDetails()).thenReturn(new ObjectMapper().createObjectNode().put("inum", inum));
        }
        try {
            when(openIdService.getIntrospectionResponse(anyString(), anyString(), isNull())).thenReturn(introspection);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static HttpServiceResponse upstream(int status, String body, String retryAfter) throws Exception {
        BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, "reason");
        if (body != null) {
            response.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }
        if (retryAfter != null) {
            response.addHeader("Retry-After", retryAfter);
        }
        return new HttpServiceResponse(new HttpPost("https://x"), response);
    }

    @Test
    public void rpIdWithColonAndSlash_isEncodedAsOneSegmentUnderTheFido2Server() {
        assertEquals(service.getReadinessUrl("https://jans.io"), BASE + "https%3A%2F%2Fjans.io/readiness");
        assertEquals(service.getReadinessUrl("a b/é"), BASE + "a%20b%2F%C3%A9/readiness");
    }

    @Test
    public void callerCannotChangeTheTarget_onlyTheEncodedSegmentVaries() {
        String url = service.getReadinessUrl("../../trust/mds/health?x=1#y");

        assertTrue(url.startsWith(BASE));
        assertTrue(url.endsWith("/readiness"));
        String segment = url.substring(BASE.length(), url.length() - "/readiness".length());
        assertFalse(segment.contains("/") || segment.contains("?") || segment.contains("#"), segment);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bearerTokenAndPrincipalAreForwarded_andBodyIsBuiltHere() throws Exception {
        when(fido2Util.executePost(anyString(), anyMap(), anyString()))
                .thenReturn(upstream(200, "{\"enabled\":true}", null));

        Outcome outcome = service.check("example.com", true, "Bearer abc");

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(fido2Util).executePost(eq(BASE + "example.com/readiness"), headers.capture(),
                eq("{\"recheck\":true}"));
        assertEquals(headers.getValue().get("Authorization"), "Bearer abc");
        assertEquals(headers.getValue().get(Constants.REQUESTED_BY_HEADER), "inum-1234");
        assertEquals(outcome.getStatus(), 200);
        assertTrue(outcome.getBody().get("enabled").asBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void missingOrHostilePrincipal_isLabelledSafely() throws Exception {
        when(fido2Util.executePost(anyString(), anyMap(), anyString())).thenReturn(upstream(200, "{}", null));

        service.check("example.com", false, null);

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(fido2Util).executePost(anyString(), headers.capture(), eq("{\"recheck\":false}"));
        assertFalse(headers.getValue().containsKey("Authorization"));
        assertFalse(headers.getValue().get(Constants.REQUESTED_BY_HEADER).contains("\n"));
        assertEquals(Fido2ReadinessService.sanitize(null), "unknown");
        assertEquals(Fido2ReadinessService.sanitize("a".repeat(500)).length(), 128);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void principal_comesFromTheIntrospectedToken_notFromTheCaller() throws Exception {
        introspect("bob\r\nX-Evil: 1", "client-1");
        when(fido2Util.executePost(anyString(), anyMap(), anyString())).thenReturn(upstream(200, "{}", null));

        service.check("example.com", false, "Bearer abc");

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(fido2Util).executePost(anyString(), headers.capture(), anyString());
        assertEquals(headers.getValue().get(Constants.REQUESTED_BY_HEADER), "bob__X-Evil:_1");
    }

    @Test
    public void introspection_isCalledWithHeaderAndToken_andNoIssuer() throws Exception {
        service.principalOf("Bearer abc");

        verify(openIdService).getIntrospectionResponse("Bearer abc", "abc", null);
        assertNull(service.principalOf("abc"));
    }

    @Test
    public void principal_prefersInumThenUsername_thenClientId_thenUnknown() throws Exception {
        introspect(null, "client-1");
        assertEquals(service.principalOf("Bearer abc"), "client-1");

        introspect(null, "alice", "client-1");
        assertEquals(service.principalOf("Bearer abc"), "alice");

        when(openIdService.getIntrospectionResponse(anyString(), anyString(), isNull()))
                .thenThrow(new IllegalStateException("down"));
        assertNull(service.principalOf("Bearer abc"));
        assertNull(service.principalOf(null));
        assertEquals(Fido2ReadinessService.sanitize(service.principalOf("Bearer abc")), "unknown");
    }

    @Test
    public void notFoundFromTheServer_isReturnedUnchanged() throws Exception {
        when(fido2Util.executePost(anyString(), anyMap(), anyString())).thenReturn(upstream(404, null, null));

        Outcome outcome = service.check("nope.example", false, "Bearer abc");

        assertEquals(outcome.getStatus(), 404);
        assertNull(outcome.getBody());
    }

    @Test
    public void tooManyRequests_nonJsonBody_keepsStatusAndRetryAfter() throws Exception {
        when(fido2Util.executePost(anyString(), anyMap(), anyString()))
                .thenReturn(upstream(429, "<html>slow down</html>", "42"));

        Outcome outcome = service.check("example.com", true, "Bearer abc");

        assertEquals(outcome.getStatus(), 429);
        assertEquals(outcome.getRetryAfter(), "42");
        assertNull(outcome.getBody());
    }

    @Test(expectedExceptions = JsonProcessingException.class)
    public void ok_nonJsonBody_isAnError() throws Exception {
        when(fido2Util.executePost(anyString(), anyMap(), anyString())).thenReturn(upstream(200, "<html/>", null));

        service.check("example.com", true, "Bearer abc");
    }

    @Test
    public void tooManyRequests_keepsRetryAfter() throws Exception {
        when(fido2Util.executePost(anyString(), anyMap(), anyString())).thenReturn(upstream(429, null, "42"));

        Outcome outcome = service.check("example.com", true, "Bearer abc");

        assertEquals(outcome.getStatus(), 429);
        assertEquals(outcome.getRetryAfter(), "42");
    }

    @Test
    public void blankId_isRejected_withoutCallingTheServer() {
        WebApplicationException e = expectThrows(WebApplicationException.class,
                () -> service.check("  ", false, "Bearer abc"));

        assertEquals(e.getResponse().getStatus(), 400);
        verify(fido2Util, never()).executePost(any(), any(), any());
    }

    @Test
    public void noResponse_isAnInternalError() {
        when(fido2Util.executePost(anyString(), anyMap(), anyString())).thenReturn(null);

        WebApplicationException e = expectThrows(WebApplicationException.class,
                () -> service.check("example.com", false, "Bearer abc"));

        assertEquals(e.getResponse().getStatus(), 500);
    }
}
