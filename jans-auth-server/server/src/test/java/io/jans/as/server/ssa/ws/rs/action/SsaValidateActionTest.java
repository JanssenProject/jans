package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaAttributes;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
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

import java.util.Calendar;
import java.util.TimeZone;

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

    @Test
    public void validate_ssaNull_422Status() {
        String jti = "e1440ecf-4b68-467c-a032-be1c43183e0c";
        when(ssaService.findSsaByJti(jti)).thenReturn(null);
        when(ssaService.createUnprocessableEntityResponse()).thenReturn(Response.status(422));

        Response response = ssaValidateAction.validate(jti);
        assertNotNull(response);
        assertEquals(response.getStatus(), 422);
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(log).warn(anyString(), eq(jti));
        verifyNoMoreInteractions(log, ssaService, errorResponseFactory);
    }

    @Test
    public void validate_ssaExpired_422Status() {
        String jti = "e1440ecf-4b68-467c-a032-be1c43183e0c";
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, -24);
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(calendar.getTime());
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);
        when(ssaService.createUnprocessableEntityResponse()).thenReturn(Response.status(422));

        Response response = ssaValidateAction.validate(jti);
        assertNotNull(response);
        assertEquals(response.getStatus(), 422);
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(log).warn(anyString(), eq(jti));
        verifyNoMoreInteractions(log, ssaService, errorResponseFactory);
    }

    @Test
    public void validate_ssaInactive_422Status() {
        String jti = "e1440ecf-4b68-467c-a032-be1c43183e0c";
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(calendar.getTime());
        ssa.setState(SsaState.USED);
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);
        when(ssaService.createUnprocessableEntityResponse()).thenReturn(Response.status(422));

        Response response = ssaValidateAction.validate(jti);
        assertNotNull(response);
        assertEquals(response.getStatus(), 422);
        verify(log).debug(anyString(), eq(jti));
        verify(errorResponseFactory).validateFeatureEnabled(any());
        verify(log).warn(anyString(), eq(jti));
        verifyNoMoreInteractions(log, ssaService, errorResponseFactory);
    }

    @Test
    public void validate_ssaWithOneTimeUseTrue_validStatus() {
        String jti = "e1440ecf-4b68-467c-a032-be1c43183e0c";
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        SsaAttributes attributes = new SsaAttributes();
        attributes.setOneTimeUse(true);
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(calendar.getTime());
        ssa.setState(SsaState.ACTIVE);
        ssa.setAttributes(attributes);
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);

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
    public void validate_ssaAttributesNullInternalServerError_badRequestResponse() {
        String jti = "e1440ecf-4b68-467c-a032-be1c43183e0c";
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(calendar.getTime());
        ssa.setState(SsaState.ACTIVE);
        ssa.setAttributes(null);
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);
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