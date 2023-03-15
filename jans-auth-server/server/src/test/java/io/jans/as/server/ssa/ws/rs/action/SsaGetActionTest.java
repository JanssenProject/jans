package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.server.service.external.ModifySsaResponseService;
import io.jans.as.server.ssa.ws.rs.SsaContextBuilder;
import io.jans.as.server.ssa.ws.rs.SsaJsonService;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaGetActionTest {

    @InjectMocks
    private SsaGetAction ssaGetAction;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Mock
    private SsaService ssaService;

    @Mock
    private SsaJsonService ssaJsonService;

    @Mock
    private SsaContextBuilder ssaContextBuilder;

    @Mock
    private ModifySsaResponseService modifySsaResponseService;

    @Mock
    private Logger log;

    @Test
    public void get_withAllParam_valid() {
        when(ssaJsonService.jsonArrayToString(any())).thenReturn("my body");
        Client client = new Client();
        client.setDn("inum=0000,ou=clients,o=jans");
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(client);

        String jti = "my-jti";
        String orgId = "org-id-test";
        Response response = ssaGetAction.get(jti, orgId, mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");
        assertNotNull(response.getEntity(), "response entity is null");
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        verify(log).debug(anyString(), any(), any());
        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(ssaContextBuilder).buildModifySsaResponseContext(any(), any());
        verify(ssaJsonService).jsonArrayToString(any());
        verify(modifySsaResponseService).get(any(), any());
        verifyNoMoreInteractions(log, errorResponseFactory);
    }

    @Test
    public void get_invalidClientAndIsErrorEnabledFalse_badRequestResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid client")
                        .build());
        doThrow(error).when(ssaRestWebServiceValidator).getClientFromSession();
        when(log.isErrorEnabled()).thenReturn(Boolean.FALSE);

        String jti = "my-jti";
        String orgId = "org-id-test";
        assertThrows(WebApplicationException.class, () -> ssaGetAction.get(jti, orgId, mock(HttpServletRequest.class)));
        verify(log).debug(anyString(), any(), any());
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(log).isErrorEnabled();
        verify(log, never()).error(anyString(), any(WebApplicationException.class));
        verifyNoMoreInteractions(log, ssaRestWebServiceValidator);
        verifyNoInteractions(ssaService, ssaContextBuilder, ssaJsonService, modifySsaResponseService);
    }

    @Test
    public void get_invalidClientAndIsErrorEnabledTrue_badRequestResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid client")
                        .build());
        doThrow(error).when(ssaRestWebServiceValidator).getClientFromSession();
        when(log.isErrorEnabled()).thenReturn(Boolean.TRUE);

        String jti = "my-jti";
        String orgId = "org-id-test";
        assertThrows(WebApplicationException.class, () -> ssaGetAction.get(jti, orgId, mock(HttpServletRequest.class)));
        verify(log).debug(anyString(), any(), any());
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(log).isErrorEnabled();
        verify(log).error(anyString(), any(WebApplicationException.class));
        verifyNoMoreInteractions(log, ssaRestWebServiceValidator);
        verifyNoInteractions(ssaService, ssaContextBuilder, ssaJsonService, modifySsaResponseService);
    }

    @Test
    public void get_invalidClientInternalServer_badRequestResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Unknown error")
                        .build());
        when(errorResponseFactory.createWebApplicationException(any(Response.Status.class), any(SsaErrorResponseType.class), anyString())).thenThrow(error);

        String jti = "my-jti";
        String orgId = "org-id-test";
        assertThrows(WebApplicationException.class, () -> ssaGetAction.get(jti, orgId, mock(HttpServletRequest.class)));
        verify(log).debug(anyString(), any(), any());
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(log, never()).isErrorEnabled();
        verify(log).error(any(), any(Exception.class));
        verify(log).error(eq(null), any(NullPointerException.class));
        verify(errorResponseFactory).createWebApplicationException(any(), any(), any());
        verifyNoMoreInteractions(log);
        verifyNoInteractions(ssaService, ssaContextBuilder, ssaJsonService, modifySsaResponseService);
    }
}