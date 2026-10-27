/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.rest.Fido2ReadinessResource;
import io.jans.configapi.plugin.fido2.service.Fido2ReadinessService.Outcome;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Fido2ReadinessResourceTest {

    private Fido2ReadinessService service;
    private Fido2ReadinessResource resource;

    @BeforeMethod
    public void setUp() throws Exception {
        service = mock(Fido2ReadinessService.class);
        resource = new Fido2ReadinessResource();
        inject("logger", mock(Logger.class));
        inject("fido2ReadinessService", service);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = Fido2ReadinessResource.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(resource, value);
    }

    private static Outcome outcome(int status, String body, String retryAfter) throws Exception {
        Constructor<Outcome> c = Outcome.class.getDeclaredConstructor(int.class, JsonNode.class, String.class);
        c.setAccessible(true);
        return c.newInstance(status, body == null ? null : new ObjectMapper().readTree(body), retryAfter);
    }

    @Test
    public void writeScope_isRequired_withAdminAndSuperWriteAsSuperScopes() throws Exception {
        Method method = Fido2ReadinessResource.class.getMethod("checkReadiness", String.class, String.class,
                String.class);
        ProtectedApi api = method.getAnnotation(ProtectedApi.class);

        assertEquals(Arrays.asList(api.scopes()), Arrays.asList(Constants.FIDO2_CONFIG_WRITE_ACCESS));
        assertTrue(Arrays.asList(api.superScopes()).contains(Constants.FIDO2_ADMIN_ACCESS));
        assertTrue(Arrays.asList(api.superScopes()).contains(ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS));
        assertEquals(api.groupScopes().length, 0, "a read scope must not be enough");
    }

    @Test
    public void tokenAndPrincipalAreHandedToTheService() throws Exception {
        when(service.check(anyString(), anyBoolean(), anyString())).thenReturn(outcome(200, "{}", null));

        Response response = resource.checkReadiness("example.com", "Bearer abc", "{\"recheck\": true}");

        assertEquals(response.getStatus(), 200);
        verify(service).check("example.com", true, "Bearer abc");
    }

    @Test
    public void emptyBody_meansNoRecheck_andUnknownFieldsAreIgnored() throws Exception {
        when(service.check(anyString(), anyBoolean(), anyString())).thenReturn(outcome(200, "{}", null));

        resource.checkReadiness("example.com", "Bearer abc", null);
        resource.checkReadiness("example.com", "Bearer abc",
                "{\"url\":\"http://169.254.169.254/\",\"host\":\"x\"}");

        verify(service, times(2)).check("example.com", false, "Bearer abc");
    }

    @Test
    public void bodyThatIsNotAJsonObject_isRejected_withoutCallingTheServer() {
        expectThrows(WebApplicationException.class,
                () -> resource.checkReadiness("example.com", "Bearer abc", "[1]"));
        expectThrows(WebApplicationException.class,
                () -> resource.checkReadiness("example.com", "Bearer abc", "{oops"));

        verifyNoInteractions(service);
    }

    @Test
    public void notFoundFromTheServer_reachesTheCaller() throws Exception {
        when(service.check(eq("nope"), anyBoolean(), anyString())).thenReturn(outcome(404, null, null));

        assertEquals(resource.checkReadiness("nope", "Bearer abc", null).getStatus(), 404);
    }

    @Test
    public void tooManyRequests_reachesTheCaller_withRetryAfter() throws Exception {
        when(service.check(anyString(), anyBoolean(), anyString())).thenReturn(outcome(429, null, "42"));

        Response response = resource.checkReadiness("example.com", "Bearer abc", null);

        assertEquals(response.getStatus(), 429);
        assertEquals(response.getHeaderString("Retry-After"), "42");
    }

    @Test
    public void anyOtherUpstreamStatus_isAnInternalError_notPassedThrough() throws Exception {
        when(service.check(anyString(), anyBoolean(), anyString()))
                .thenReturn(outcome(502, "{\"secret\":1}", null));

        WebApplicationException e = expectThrows(WebApplicationException.class,
                () -> resource.checkReadiness("example.com", "Bearer abc", null));

        assertEquals(e.getResponse().getStatus(), 500);
    }
}
