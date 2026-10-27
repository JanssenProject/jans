/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.readiness.NativeReadinessService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NativeReadinessControllerTest {

    private static final String RP_ID = "https://jans.io";
    private static final String MARKER = "SECRET-MARKER-9f3a";

    @InjectMocks
    private NativeReadinessController controller;

    @Mock
    private Logger log;
    @Mock
    private NativeReadinessService readinessService;
    @Mock
    private DataMapperService dataMapperService;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void emptyBody_runsACachedCheck() throws Exception {
        when(readinessService.check(RP_ID, false, "alice")).thenReturn(null);
        when(dataMapperService.writeValueAsString(any())).thenReturn("{}");

        Response response = controller.check(RP_ID, "alice", null);

        assertEquals(200, response.getStatus());
        verify(readinessService).check(RP_ID, false, "alice");
        assertEquals(200, controller.check(RP_ID, "alice", "  ").getStatus());
    }

    @Test
    void recheckTrue_isPassedOn() throws Exception {
        when(dataMapperService.writeValueAsString(any())).thenReturn("{}");

        controller.check(RP_ID, null, "{\"recheck\": true}");

        verify(readinessService).check(RP_ID, true, null);
    }

    @Test
    void fieldsThatCouldNameAHost_areIgnored_andNeverReachTheService() throws Exception {
        when(dataMapperService.writeValueAsString(any())).thenReturn("{}");

        controller.check(RP_ID, null,
                "{\"url\":\"http://169.254.169.254/\",\"host\":\"evil.example\",\"port\":8080,\"path\":\"/x\"}");

        // The only inputs the service ever receives are the configured-id selector, the flag and the label.
        verify(readinessService).check(RP_ID, false, null);
    }

    @Test
    void bodyThatIsNotAJsonObject_isRejected_withoutRunningACheck() {
        when(errorResponseFactory.invalidRequest(anyString()))
                .thenReturn(new WebApplicationException(Response.Status.BAD_REQUEST));

        assertThrows(WebApplicationException.class, () -> controller.check(RP_ID, null, "[1,2]"));
        assertThrows(WebApplicationException.class, () -> controller.check(RP_ID, null, "{not json"));

        verifyNoInteractions(readinessService);
    }

    @Test
    void unknownRp_is404() {
        when(readinessService.check(anyString(), anyBoolean(), any()))
                .thenThrow(new NativeReadinessService.NotFound());

        assertEquals(404, controller.check("nope", null, null).getStatus());
    }

    @Test
    void rateLimited_is429_withRetryAfter() {
        when(readinessService.check(anyString(), anyBoolean(), any()))
                .thenThrow(new NativeReadinessService.RateLimited(42));

        Response response = controller.check(RP_ID, null, null);

        assertEquals(429, response.getStatus());
        assertEquals(42L, response.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void unexpectedFailure_isSanitized() {
        when(readinessService.check(anyString(), anyBoolean(), any()))
                .thenThrow(new IllegalStateException("jdbc://" + MARKER));
        when(errorResponseFactory.unknownError(anyString()))
                .thenReturn(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));

        WebApplicationException e = assertThrows(WebApplicationException.class,
                () -> controller.check(RP_ID, null, null));

        verify(errorResponseFactory).unknownError("Failed to run native app readiness checks");
        assertFalse(String.valueOf(e.getResponse().getEntity()).contains(MARKER));
    }
}
