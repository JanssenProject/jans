package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.ssa.ws.rs.SsaJsonService;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaGetJwtActionTest {

    @InjectMocks
    private SsaGetJwtAction ssaGetJwtAction;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private SsaService ssaService;

    @Mock
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Mock
    private SsaJsonService ssaJsonService;

    @Test
    public void testGetJwtSsa_jti_validStatus() throws Exception {
        String jti = "test-jti";
        String jwt = "jwt-test";
        Ssa ssa = new Ssa();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ssa", jwt);
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(mock(Client.class));
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenReturn(ssa);
        when(ssaService.generateJwt(ssa)).thenReturn(mock(Jwt.class));
        when(ssaJsonService.getJSONObject(any())).thenReturn(jsonObject);
        when(ssaJsonService.jsonObjectToString(jsonObject)).thenReturn(jsonObject.toString());

        Response response = ssaGetJwtAction.getJwtSsa(jti);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        assertNotNull(response.getEntity());
        JSONObject aux = new JSONObject(response.getEntity().toString());
        assertTrue(aux.has("ssa"));

        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(), anyString());
        verifyNoMoreInteractions(log, errorResponseFactory);
    }

    @Test
    public void testGetJwtSsa_jwtWithErrorEnabledFalse_422Status() {
        String jti = "test-jti";
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(mock(Client.class));
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenThrow(new WebApplicationException(Response.status(422).build()));
        when(log.isErrorEnabled()).thenReturn(false);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaGetJwtAction.getJwtSsa(jti));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 422);

        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(), anyString());
        verifyNoInteractions(ssaService, ssaJsonService);
        verifyNoMoreInteractions(log, errorResponseFactory);
    }

    @Test
    public void testGetJwtSsa_jwtWithErrorEnabledTrue_422Status() {
        String jti = "test-jti";
        when(ssaRestWebServiceValidator.getClientFromSession()).thenReturn(mock(Client.class));
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenThrow(new WebApplicationException(Response.status(422).build()));
        when(log.isErrorEnabled()).thenReturn(true);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaGetJwtAction.getJwtSsa(jti));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 422);

        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(ssaRestWebServiceValidator).checkScopesPolicy(any(), anyString());
        verifyNoInteractions(ssaService, ssaJsonService);
        verify(log).error(anyString(), eq(ex));
        verifyNoMoreInteractions(log, errorResponseFactory);
    }

    @Test
    public void testGetJwtSsa_jtiNullPointerException_500Status() {
        String jti = "test-jti";
        when(ssaRestWebServiceValidator.getClientFromSession()).thenThrow(new NullPointerException("test null message"));
        when(errorResponseFactory.createWebApplicationException(any(), any(), anyString())).thenThrow(new WebApplicationException(Response.status(500).build()));

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaGetJwtAction.getJwtSsa(jti));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 500);

        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(log).error(anyString(), any(Exception.class));
        verifyNoMoreInteractions(ssaRestWebServiceValidator, log);
        verifyNoInteractions(ssaService, ssaJsonService);
    }
}