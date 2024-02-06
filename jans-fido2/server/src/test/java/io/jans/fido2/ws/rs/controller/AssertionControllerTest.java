package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.sg.converter.AssertionSuperGluuController;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssertionControllerTest {

    @InjectMocks
    private AssertionController assertionController;

    @Mock
    private Logger log;

    @Mock
    private AssertionService assertionService;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private AssertionSuperGluuController assertionSuperGluuController;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void authenticate_ifFido2ConfigurationIsNull_forbiddenException() {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.authenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, assertionService, log);
    }

    @Test
    void authenticate_ifReadTreeThrownError_invalidRequest() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(dataMapperService.readTree(anyString())).thenThrow(new IOException("IOException test error"));
        when(errorResponseFactory.invalidRequest(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.authenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(commonVerifiers, assertionService, log);
    }

    @Test
    void authenticate_ifThrownException_unknownError() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.authenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(assertionService).options(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void authenticate_ifValidData_success() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenReturn(mock(ObjectNode.class));

        Response response = assertionController.authenticate(content);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(assertionService).options(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void generateAuthenticate_ifFido2ConfigurationIsNull_forbiddenException() {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.generateAuthenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, assertionService, log);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void generateAuthenticate_ifAssertionOptionsGenerateEndpointEnabledIsFalse_forbiddenException() {
        String content = "test_content";
        Fido2Configuration fido2Configuration = mock(Fido2Configuration.class);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.isAssertionOptionsGenerateEndpointEnabled()).thenReturn(false);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.generateAuthenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration, times(2)).getFido2Configuration();
        verifyNoInteractions(dataMapperService, assertionService, log);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void generateAuthenticate_ifReadTreeThrownError_invalidRequest() throws IOException {
        String content = "test_content";
        Fido2Configuration fido2Configuration = mock(Fido2Configuration.class);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.isAssertionOptionsGenerateEndpointEnabled()).thenReturn(true);
        when(dataMapperService.readTree(anyString())).thenThrow(new IOException("IOException test error"));
        when(errorResponseFactory.invalidRequest(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.generateAuthenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration, times(2)).getFido2Configuration();
        verifyNoInteractions(assertionService, log);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void generateAuthenticate_ifThrownException_unknownError() throws IOException {
        String content = "test_content";
        Fido2Configuration fido2Configuration = mock(Fido2Configuration.class);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.isAssertionOptionsGenerateEndpointEnabled()).thenReturn(true);
        when(dataMapperService.readTree(anyString())).thenReturn(mock(JsonNode.class));
        when(assertionService.generateOptions(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.generateAuthenticate(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration, times(2)).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(dataMapperService).readTree(content);
        verify(assertionService).generateOptions(any());
        verifyNoMoreInteractions(errorResponseFactory, dataMapperService, assertionService, appConfiguration, log);
    }

    @Test
    void generateAuthenticate_ifValidData_success() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenReturn(mock(ObjectNode.class));

        Response response = assertionController.authenticate(content);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(assertionService).options(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void verify_ifFido2ConfigurationIsNull_forbiddenException() {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.verify(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, assertionService, log);
    }

    @Test
    void verify_ifReadTreeThrownError_invalidRequest() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(dataMapperService.readTree(anyString())).thenThrow(new IOException("IOException test error"));
        when(errorResponseFactory.invalidRequest(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.verify(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(commonVerifiers, assertionService, log);
    }

    @Test
    void verify_ifThrownException_unknownError() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.verify(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.verify(content));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(assertionService).verify(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void verify_ifValidData_success() throws IOException {
        String content = "test_content";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.verify(any())).thenReturn(mock(ObjectNode.class));

        Response response = assertionController.verify(content);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(dataMapperService).readTree(content);
        verify(commonVerifiers).verifyNotUseGluuParameters(any());
        verify(assertionService).verify(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void startAuthentication_ifFido2ConfigurationIsNullAndSuperGluuEnabledIsFalse_forbiddenException() {
        String userName = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(false);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.startAuthentication(userName, keyHandle, appId, sessionId));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verifyNoInteractions(log, assertionSuperGluuController);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void startAuthentication_ifFidoConfigurationNotNullAndThrownError_unknownError() {
        String userName = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionSuperGluuController.startAuthentication(any(), any(), any(), any())).thenThrow(new RuntimeException("Runtime test error"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.startAuthentication(userName, keyHandle, appId, sessionId));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration, never()).isSuperGluuEnabled();
        verify(log).debug("Start authentication: username = {}, keyhandle = {}, application = {}, session_id = {}", userName, keyHandle, appId, sessionId);
        verify(log).error(contains("Unknown Error"), any(), any());
        verifyNoMoreInteractions(appConfiguration, log);
    }

    @Test
    void startAuthentication_ifFidoConfigurationIsNullAndSuperGluuEnabledIsTrue_success() {
        String userName = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(true);
        when(assertionSuperGluuController.startAuthentication(any(), any(), any(), any())).thenReturn(mock(ObjectNode.class));

        Response response = assertionController.startAuthentication(userName, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verify(assertionSuperGluuController).startAuthentication(userName, keyHandle, appId, sessionId);
        verify(log).debug("Start authentication: username = {}, keyhandle = {}, application = {}, session_id = {}", userName, keyHandle, appId, sessionId);
        verify(log).debug(contains("Prepared U2F_V2 authentication options request"), anyString());
        verifyNoInteractions(errorResponseFactory);
        verifyNoMoreInteractions(log);
    }

    @Test
    void finishAuthentication_ifFido2ConfigurationIsNullAndSuperGluuEnabledIsFalse_forbiddenException() {
        String userName = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(false);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.finishAuthentication(userName, authenticateResponseString));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verifyNoInteractions(log, assertionSuperGluuController);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void finishAuthentication_ifFidoConfigurationNotNullAndThrownError_unknownError() {
        String userName = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionSuperGluuController.finishAuthentication(any(), any())).thenThrow(new RuntimeException("Runtime test error"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.finishAuthentication(userName, authenticateResponseString));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration, never()).isSuperGluuEnabled();
        verify(log).debug("Finish authentication: username = {}, tokenResponse = {}", userName, authenticateResponseString);
        verify(log).error(contains("Unknown Error"), any(), any());
        verifyNoMoreInteractions(appConfiguration, log);
    }

    @Test
    void finishAuthentication_ifFidoConfigurationIsNullAndSuperGluuEnabledIsTrue_success() {
        String userName = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(true);
        when(assertionSuperGluuController.finishAuthentication(any(), any())).thenReturn(mock(ObjectNode.class));

        Response response = assertionController.finishAuthentication(userName, authenticateResponseString);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verify(assertionSuperGluuController).finishAuthentication(userName, authenticateResponseString);
        verify(log).debug("Finish authentication: username = {}, tokenResponse = {}", userName, authenticateResponseString);
        verify(log).debug(contains("Prepared U2F_V2 authentication verify request"), anyString());
        verifyNoInteractions(errorResponseFactory);
        verifyNoMoreInteractions(log);
    }
}
