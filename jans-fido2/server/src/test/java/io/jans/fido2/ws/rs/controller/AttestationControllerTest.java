package io.jans.fido2.ws.rs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.attestation.PublicKeyCredentialCreationOptions;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class AttestationControllerTest {

    @InjectMocks
    private AttestationController attestationController;

    @Mock
    private Logger log;

    @Mock
    private AttestationService attestationService;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void register_ifFido2ConfigurationIsNull_forbiddenException() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.register(mock(AttestationOptions.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, attestationService, log);
    }
//
    @Test
    void register_ifReadTreeThrownError_invalidRequest() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.options(any())).thenThrow(new BadRequestException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.register(mock(AttestationOptions.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(log);
    }

    @Test
    void register_ifThrownException_unknownError() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.options(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.register(mock(AttestationOptions.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(attestationService).options(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void register_ifValidData_success() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.options(any())).thenReturn(mock(PublicKeyCredentialCreationOptions.class));

        Response response = attestationController.register(mock(AttestationOptions.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(attestationService).options(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void verify_ifFido2ConfigurationIsNull_forbiddenException() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.verify(mock(AttestationResult.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, attestationService, log);
    }

    @Test
    void verify_ifReadTreeThrownError_invalidRequest() throws IOException {

        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.verify(any())).thenThrow(new BadRequestException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.verify(mock(AttestationResult.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(log);
    }

    @Test
    void verify_ifThrownException_unknownError() throws IOException {
        AttestationResult AttestationResult = new AttestationResult();
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.verify(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.verify(mock(AttestationResult.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(attestationService).verify(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void verify_ifValidData_success() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.verify(any())).thenReturn(mock(AttestationOrAssertionResponse.class));

        Response response = attestationController.verify(mock(AttestationResult.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(attestationService).verify(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

}
