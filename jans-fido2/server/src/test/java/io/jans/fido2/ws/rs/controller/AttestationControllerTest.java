package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.sg.converter.AttestationSuperGluuController;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private AttestationSuperGluuController attestationSuperGluuController;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void register_ifFido2ConfigurationIsNull_forbiddenException() {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.register(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, attestationService, log);
    }

    @Test
    void register_ifReadTreeThrownError_invalidRequest() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(dataMapperService.readTree(anyString())).thenThrow(new IOException("IOException test error"));
        when(errorResponseFactory.invalidRequest(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.register(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(commonVerifiers, attestationService, log);
    }

    @Test
    void register_ifThrownException_unknownError() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.options(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.register(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(attestationService).options(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void register_ifValidData_success() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.options(any())).thenReturn(mock(ObjectNode.class));

        Response response = attestationController.register(content);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(attestationService).options(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void verify_ifFido2ConfigurationIsNull_forbiddenException() {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.verify(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, attestationService, log);
    }

    @Test
    void verify_ifReadTreeThrownError_invalidRequest() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(dataMapperService.readTree(anyString())).thenThrow(new IOException("IOException test error"));
        when(errorResponseFactory.invalidRequest(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.verify(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(commonVerifiers, attestationService, log);
    }

    @Test
    void verify_ifThrownException_unknownError() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.verify(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.verify(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(attestationService).verify(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void verify_ifValidData_success() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationService.verify(any())).thenReturn(mock(ObjectNode.class));

        Response response = attestationController.verify(content);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(attestationService).verify(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void startRegistration_ifFido2ConfigurationIsNullAndSuperGluuEnabledIsFalse_forbiddenException() {
        String userName = "test_username";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        String enrollmentCode = "test_enrollment_code";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(false);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.startRegistration(userName, appId, sessionId, enrollmentCode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verifyNoInteractions(log, attestationSuperGluuController);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void startRegistration_ifFidoConfigurationNotNullAndThrownError_unknownError() {
        String userName = "test_username";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        String enrollmentCode = "test_enrollment_code";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationSuperGluuController.startRegistration(any(), any(), any(), any())).thenThrow(new RuntimeException("Runtime test error"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.startRegistration(userName, appId, sessionId, enrollmentCode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration, never()).isSuperGluuEnabled();
        verify(log).debug("Start registration: username = {}, application = {}, session_id = {}, enrollment_code = {}", userName, appId, sessionId, enrollmentCode);
        verify(log).error(contains("Unknown Error"), any(), any());
        verifyNoMoreInteractions(appConfiguration, log);
    }

    @Test
    void startRegistration_ifFidoConfigurationIsNullAndSuperGluuEnabledIsTrue_success() {
        String userName = "test_username";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        String enrollmentCode = "test_enrollment_code";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(true);
        when(attestationSuperGluuController.startRegistration(any(), any(), any(), any())).thenReturn(mock(ObjectNode.class));

        Response response = attestationController.startRegistration(userName, appId, sessionId, enrollmentCode);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verify(attestationSuperGluuController).startRegistration(userName, appId, sessionId, enrollmentCode);
        verify(log).debug("Start registration: username = {}, application = {}, session_id = {}, enrollment_code = {}", userName, appId, sessionId, enrollmentCode);
        verify(log).debug(contains("Prepared U2F_V2 registration options request"), anyString());
        verifyNoInteractions(errorResponseFactory);
        verifyNoMoreInteractions(log);
    }

    @Test
    void finishRegistration_ifFido2ConfigurationIsNullAndSuperGluuEnabledIsFalse_forbiddenException() {
        String userName = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(false);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.finishRegistration(userName, authenticateResponseString));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verifyNoInteractions(log, attestationSuperGluuController);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void finishRegistration_ifFidoConfigurationNotNullAndThrownError_unknownError() {
        String userName = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(attestationSuperGluuController.finishRegistration(any(), any())).thenThrow(new RuntimeException("Runtime test error"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationController.finishRegistration(userName, authenticateResponseString));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration, never()).isSuperGluuEnabled();
        verify(log).debug("Finish registration: username = {}, tokenResponse = {}", userName, authenticateResponseString);
        verify(log).error(contains("Unknown Error"), any(), any());
        verifyNoMoreInteractions(appConfiguration, log);
    }

    @Test
    void finishRegistration_ifFidoConfigurationIsNullAndSuperGluuEnabledIsTrue_success() {
        String userName = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(true);
        when(attestationSuperGluuController.finishRegistration(any(), any())).thenReturn(mock(ObjectNode.class));

        Response response = attestationController.finishRegistration(userName, authenticateResponseString);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verify(attestationSuperGluuController).finishRegistration(userName, authenticateResponseString);
        verify(log).debug("Finish registration: username = {}, tokenResponse = {}", userName, authenticateResponseString);
        verify(log).debug(contains("Prepared U2F_V2 registration verify request"), anyString());
        verifyNoInteractions(errorResponseFactory);
        verifyNoMoreInteractions(log);
    }
}
