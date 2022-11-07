package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.service.AttributeService;
import io.jans.as.server.service.external.ModifySsaResponseService;
import io.jans.as.server.ssa.ws.rs.SsaContextBuilder;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaRevokeActionTest {

    @InjectMocks
    private SsaRevokeAction ssaRevokeAction;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private SsaService ssaService;

    @Mock
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Mock
    private ModifySsaResponseService modifySsaResponseService;

    @Mock
    private SsaContextBuilder ssaContextBuilder;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private AttributeService attributeService;

    @Test
    public void revoke_notValidParam_406Status() {
        when(ssaService.createNotAcceptableResponse()).thenReturn(Response.status(406));

        String jti = null;
        Long orgId = null;
        Response response = ssaRevokeAction.revoke(jti, orgId, mock(HttpServletRequest.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 406);
        verify(log).debug(anyString(), eq(jti), eq(orgId));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verifyNoMoreInteractions(ssaRestWebServiceValidator, ssaService, log, errorResponseFactory);
        verifyNoInteractions(ssaContextBuilder, modifySsaResponseService, appConfiguration, attributeService);
    }

    @Test
    public void revoke_ssaListEmpty_422Status() {
        Client client = new Client();
        client.setDn("inum=0000,ou=clients,o=jans");
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(client);
        when(ssaService.createUnprocessableEntityResponse()).thenReturn(Response.status(422));

        String jti = "test-jti";
        Long orgId = 1000L;
        Response response = ssaRevokeAction.revoke(jti, orgId, mock(HttpServletRequest.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 422);
        verify(log).debug(anyString(), eq(jti), eq(orgId));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(Client.class), anyString());
        verify(ssaService).getSsaList(anyString(), any(), any(), any(), any());
        verifyNoMoreInteractions(ssaService, log, errorResponseFactory);
        verifyNoInteractions(ssaContextBuilder, modifySsaResponseService, appConfiguration, attributeService);
    }

    @Test
    public void revoke_validJti_200Status() {
        String jti = "test-jti";
        Long orgId = null;
        Client client = new Client();
        client.setDn("inum=0000,ou=clients,o=jans");
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(client);
        Ssa ssa = new Ssa();
        ssa.setId(jti);
        when(ssaService.getSsaList(anyString(), any(), any(), any(), any())).thenReturn(Collections.singletonList(ssa));

        Response response = ssaRevokeAction.revoke(jti, orgId, mock(HttpServletRequest.class));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        verify(log).debug(anyString(), eq(jti), eq(orgId));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(Client.class), anyString());

        ArgumentCaptor<Ssa> ssaCaptor = ArgumentCaptor.forClass(Ssa.class);
        verify(ssaService).merge(ssaCaptor.capture());
        Ssa ssaAux = ssaCaptor.getValue();
        assertNotNull(ssaAux, "Ssa after merge is null");
        assertNotNull(ssaAux.getState(), "Ssa state is null");
        assertEquals(ssa.getState(), SsaState.REVOKED);

        verify(log).info(anyString(), eq(jti), eq(SsaState.REVOKED.getValue()));
        verify(ssaContextBuilder).buildModifySsaResponseContext(any(), any(), any(), any(), any());
        verify(modifySsaResponseService).revoke(any(), any());
        verifyNoMoreInteractions(ssaService, log, errorResponseFactory);
    }

    @Test
    public void revoke_invalidClientAndIsErrorEnabledFalse_badRequestResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid client")
                        .build());
        doThrow(error).when(ssaRestWebServiceValidator).getClientFromSession();
        when(log.isErrorEnabled()).thenReturn(Boolean.FALSE);

        String jti = "test-jti";
        Long orgId = 1000L;
        assertThrows(WebApplicationException.class, () -> ssaRevokeAction.revoke(jti, orgId, mock(HttpServletRequest.class)));
        verify(log).debug(anyString(), eq(jti), eq(orgId));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(log).isErrorEnabled();
        verify(log, never()).error(anyString(), any(WebApplicationException.class));
        verifyNoMoreInteractions(log, errorResponseFactory, ssaService, ssaRestWebServiceValidator);
        verifyNoInteractions(ssaContextBuilder, modifySsaResponseService, appConfiguration, attributeService);
    }

    @Test
    public void revoke_invalidClientAndIsErrorEnabledTrue_badRequestResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid client")
                        .build());
        doThrow(error).when(ssaRestWebServiceValidator).getClientFromSession();
        when(log.isErrorEnabled()).thenReturn(Boolean.TRUE);

        String jti = "test-jti";
        Long orgId = 1000L;
        assertThrows(WebApplicationException.class, () -> ssaRevokeAction.revoke(jti, orgId, mock(HttpServletRequest.class)));
        verify(log).debug(anyString(), eq(jti), eq(orgId));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(log).isErrorEnabled();
        verify(log).error(anyString(), any(WebApplicationException.class));
        verifyNoMoreInteractions(log, errorResponseFactory, ssaService, ssaRestWebServiceValidator);
        verifyNoInteractions(ssaContextBuilder, modifySsaResponseService, appConfiguration, attributeService);
    }

    @Test
    public void revoke_withNullPointException_badRequestResponse() {
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Unknown error")
                        .build());
        when(errorResponseFactory.createWebApplicationException(any(), any(), anyString())).thenThrow(error);

        String jti = "test-jti";
        Long orgId = 1000L;
        assertThrows(WebApplicationException.class, () -> ssaRevokeAction.revoke(jti, orgId, mock(HttpServletRequest.class)));
        verify(log).debug(anyString(), eq(jti), eq(orgId));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(log).error(eq(null), any(NullPointerException.class));
        verify(ssaRestWebServiceValidator).getClientFromSession();
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(), anyString());
        verifyNoMoreInteractions(log, errorResponseFactory, ssaRestWebServiceValidator, ssaService);
        verifyNoInteractions(ssaContextBuilder, modifySsaResponseService, appConfiguration, attributeService);
    }
}