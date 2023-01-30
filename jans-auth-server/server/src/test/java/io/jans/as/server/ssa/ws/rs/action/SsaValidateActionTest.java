package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaAttributes;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.ArgumentCaptor;
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
public class SsaValidateActionTest {

    @InjectMocks
    private SsaValidateAction ssaValidateAction;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private SsaService ssaService;

    @Mock
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Test
    public void validate_ssaWithOneTimeUseFalse_validStatus() {
        String jti = "test-jti";
        SsaAttributes attributes = new SsaAttributes();
        attributes.setOneTimeUse(false);
        Ssa ssa = new Ssa();
        ssa.setState(SsaState.ACTIVE);
        ssa.setAttributes(attributes);
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenReturn(ssa);

        Response response = ssaValidateAction.validate(jti);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(FeatureFlagType.SSA);
        verifyNoInteractions(ssaService);
        verifyNoMoreInteractions(log, errorResponseFactory);
    }

    @Test
    public void validate_ssaWithOneTimeUseTrue_validStatus() {
        String jti = "test-jti";
        SsaAttributes attributes = new SsaAttributes();
        attributes.setOneTimeUse(true);
        Ssa ssa = new Ssa();
        ssa.setState(SsaState.ACTIVE);
        ssa.setAttributes(attributes);
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenReturn(ssa);

        Response response = ssaValidateAction.validate(jti);
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(ssaService, never()).createUnprocessableEntityResponse();
        verify(log).info(anyString(), any(), any());
        verifyNoMoreInteractions(log, errorResponseFactory);

        ArgumentCaptor<Ssa> ssaCaptor = ArgumentCaptor.forClass(Ssa.class);
        verify(ssaService).merge(ssaCaptor.capture());
        Ssa ssaAux = ssaCaptor.getValue();
        assertNotNull(ssaAux.getState(), "ssa state is null");
        assertEquals(ssaAux.getState(), SsaState.USED);
    }

    @Test
    public void validate_ssaInvalidWithErrorEnabledFalse_422Status() {
        String jti = "test-jti";
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenThrow(new WebApplicationException(Response.status(422).build()));
        when(log.isErrorEnabled()).thenReturn(false);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaValidateAction.validate(jti));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 422);
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verifyNoMoreInteractions(ssaRestWebServiceValidator, log, errorResponseFactory, ssaService);
    }

    @Test
    public void validate_ssaInvalidWithErrorEnabledTrue_422Status() {
        String jti = "test-jti";
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenThrow(new WebApplicationException(Response.status(422).build()));
        when(log.isErrorEnabled()).thenReturn(true);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaValidateAction.validate(jti));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 422);
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(eq(FeatureFlagType.SSA));
        verify(log).error(anyString(), eq(ex));
        verifyNoMoreInteractions(log, errorResponseFactory, ssaService);
    }

    @Test
    public void validate_ssaAttributesNullInternalServerError_badRequestResponse() {
        String jti = "test-jti";
        Ssa ssa = new Ssa();
        ssa.setState(SsaState.ACTIVE);
        ssa.setAttributes(null);
        when(ssaRestWebServiceValidator.getValidSsaByJti(jti)).thenReturn(ssa);
        WebApplicationException error = new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Unknown error")
                        .build());
        when(errorResponseFactory.createWebApplicationException(any(Response.Status.class), any(SsaErrorResponseType.class), anyString())).thenThrow(error);

        assertThrows(WebApplicationException.class, () -> ssaValidateAction.validate(jti));
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(log).error(eq(null), any(NullPointerException.class));
        verifyNoMoreInteractions(log, ssaService, errorResponseFactory);
    }
}