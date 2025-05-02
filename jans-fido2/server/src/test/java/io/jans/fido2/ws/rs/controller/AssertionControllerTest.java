package io.jans.fido2.ws.rs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
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

import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionOptionsResponse;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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
    private AppConfiguration appConfiguration;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void authenticate_ifFido2ConfigurationIsNull_forbiddenException() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.authenticate(mock(AssertionOptions.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, assertionService, log);
    }

    @Test
    void authenticate_ifReadTreeThrownError_invalidRequest() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenThrow(new BadRequestException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.authenticate(mock(AssertionOptions.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(log);
    }

    @Test
    void authenticate_ifThrownException_unknownError() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.authenticate(mock(AssertionOptions.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(assertionService).options(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void authenticate_ifValidData_success() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenReturn(mock(AssertionOptionsResponse.class));

        Response response = assertionController.authenticate(mock(AssertionOptions.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(assertionService).options(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    // TODO: delete after fixing the defect concerning isAssertionOptionsGenerateEndpointEnabled
	/*
	 * @Test void
	 * generateAuthenticate_ifFido2ConfigurationIsNull_forbiddenException() {
	 * when(appConfiguration.getFido2Configuration()).thenReturn(null);
	 * when(errorResponseFactory.forbiddenException()).thenReturn(new
	 * WebApplicationException(Response.status(500).entity("test exception").build()
	 * ));
	 * 
	 * WebApplicationException ex = assertThrows(WebApplicationException.class, ()
	 * ->
	 * assertionController.generateAuthenticate(mock(AssertionOptionsGenerate.class)
	 * )); assertNotNull(ex); assertNotNull(ex.getResponse());
	 * assertEquals(ex.getResponse().getStatus(), 500);
	 * assertEquals(ex.getResponse().getEntity(), "test exception");
	 * 
	 * verify(appConfiguration).getFido2Configuration();
	 * verifyNoInteractions(dataMapperService, assertionService, log);
	 * verifyNoMoreInteractions(errorResponseFactory); }
	 * 
	 * @Test void
	 * generateAuthenticate_ifAssertionOptionsGenerateEndpointEnabledIsFalse_forbiddenException
	 * () { Fido2Configuration fido2Configuration = mock(Fido2Configuration.class);
	 * when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration)
	 * ; when(fido2Configuration.isAssertionOptionsGenerateEndpointEnabled()).
	 * thenReturn(false);
	 * when(errorResponseFactory.forbiddenException()).thenReturn(new
	 * WebApplicationException(Response.status(500).entity("test exception").build()
	 * ));
	 * 
	 * WebApplicationException ex = assertThrows(WebApplicationException.class, ()
	 * ->
	 * assertionController.generateAuthenticate(mock(AssertionOptionsGenerate.class)
	 * )); assertNotNull(ex); assertNotNull(ex.getResponse());
	 * assertEquals(ex.getResponse().getStatus(), 500);
	 * assertEquals(ex.getResponse().getEntity(), "test exception");
	 * 
	 * verify(appConfiguration, times(2)).getFido2Configuration();
	 * verifyNoInteractions(dataMapperService, assertionService, log);
	 * verifyNoMoreInteractions(errorResponseFactory); }
	 */
//
	/*
	 * @ Test void generateAuthenticate_ifReadTreeThrownError_invalidRequest() throws
	 * IOException { Fido2Configuration fido2Configuration =
	 * mock(Fido2Configuration.class);
	 * when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration)
	 * ; when(fido2Configuration.isAssertionOptionsGenerateEndpointEnabled()).
	 * thenReturn(true); when(assertionService.generateOptions(any())).thenThrow(new
	 * BadRequestException(Response.status(400).entity("test exception").build()));
	 * 
	 * WebApplicationException ex = assertThrows(WebApplicationException.class, ()
	 * ->
	 * assertionController.generateAuthenticate(mock(AssertionOptionsGenerate.class)
	 * )); assertNotNull(ex); assertNotNull(ex.getResponse());
	 * assertEquals(ex.getResponse().getStatus(), 400);
	 * assertEquals(ex.getResponse().getEntity(), "test exception");
	 * 
	 * verify(appConfiguration, times(2)).getFido2Configuration();
	 * verifyNoInteractions(log); verifyNoMoreInteractions(errorResponseFactory); }
	 * 
	 * @ Test void generateAuthenticate_ifThrownException_unknownError() throws
	 * IOException { Fido2Configuration fido2Configuration =
	 * mock(Fido2Configuration.class);
	 * when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration)
	 * ; when(fido2Configuration.isAssertionOptionsGenerateEndpointEnabled()).
	 * thenReturn(true);
	 * //when(dataMapperService.readTree(anyString())).thenReturn(mock(JsonNode.
	 * class)); when(assertionService.generateOptions(any())).thenThrow(new
	 * RuntimeException("test exception"));
	 * when(errorResponseFactory.unknownError(any())).thenReturn(new
	 * WebApplicationException(Response.status(500).entity("test exception").build()
	 * ));
	 * 
	 * WebApplicationException ex = assertThrows(WebApplicationException.class, ()
	 * ->
	 * assertionController.generateAuthenticate(mock(AssertionOptionsGenerate.class)
	 * )); assertNotNull(ex); assertNotNull(ex.getResponse());
	 * assertEquals(ex.getResponse().getStatus(), 500);
	 * assertEquals(ex.getResponse().getEntity(), "test exception");
	 * 
	 * verify(appConfiguration, times(2)).getFido2Configuration();
	 * verify(log).error(contains("Unknown Error"), any(), any());
	 * verify(assertionService).generateOptions(any());
	 * verifyNoMoreInteractions(errorResponseFactory, dataMapperService,
	 * assertionService, appConfiguration, log); }
	 */

    @Test
    void generateAuthenticate_ifValidData_success() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.options(any())).thenReturn(mock(AssertionOptionsResponse.class));

        Response response = assertionController.authenticate(mock(AssertionOptions.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(assertionService).options(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void verify_ifFido2ConfigurationIsNull_forbiddenException() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.verify(mock(AssertionResult.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService, commonVerifiers, assertionService, log);
    }

    @Test
    void verify_ifReadTreeThrownError_invalidRequest() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        //when(assertionService.verify(any())).thenThrow(new RuntimeException("test exception"));
        when(assertionService.verify(any())).thenThrow(new BadRequestException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.verify(mock(AssertionResult.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(log);
    }

    @Test
    void verify_ifThrownException_unknownError() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.verify(any())).thenThrow(new RuntimeException("test exception"));
        when(errorResponseFactory.unknownError(any())).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionController.verify(mock(AssertionResult.class)));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(log).error(contains("Unknown Error"), any(), any());
        verify(assertionService).verify(any());
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    void verify_ifValidData_success() throws IOException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(assertionService.verify(any())).thenReturn(mock(AttestationOrAssertionResponse.class));

        Response response = assertionController.verify(mock(AssertionResult.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(appConfiguration).getFido2Configuration();
        verify(assertionService).verify(any());
        verifyNoInteractions(log, errorResponseFactory);
    }

   

   
}
