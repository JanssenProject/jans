/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.trust.AttestationTrustConfig;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.trust.TrustStatusService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Fido2TrustControllerTest {

    @InjectMocks
    private Fido2TrustController fido2TrustController;

    @Mock
    private Logger log;
    @Mock
    private TrustStatusService trustStatusService;
    @Mock
    private DataMapperService dataMapperService;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void getAttestationConfig_returnsOk() throws Exception {
        when(trustStatusService.getAttestationTrustConfig()).thenReturn(new AttestationTrustConfig());
        when(dataMapperService.writeValueAsString(any())).thenReturn("{}");

        Response response = fido2TrustController.getAttestationConfig();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void getAttestationConfig_ifServiceFails_raisesSanitizedError() {
        when(trustStatusService.getAttestationTrustConfig())
                .thenThrow(new IllegalStateException("jdbc://secret-host/db"));
        when(errorResponseFactory.unknownError(anyString()))
                .thenReturn(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));

        assertThrows(WebApplicationException.class, () -> fido2TrustController.getAttestationConfig());

        // The internal detail goes to the log, never into the response.
        verify(errorResponseFactory).unknownError("Failed to read trust status");
    }
}
