package io.jans.as.server.ssa.ws.rs;

import io.jans.as.server.ssa.ws.rs.action.SsaCreateAction;
import io.jans.as.server.ssa.ws.rs.action.SsaGetAction;
import io.jans.as.server.ssa.ws.rs.action.SsaRevokeAction;
import io.jans.as.server.ssa.ws.rs.action.SsaValidateAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNotNull;

@Listeners(MockitoTestNGListener.class)
public class SsaRestWebServiceImplTest {

    @InjectMocks
    private SsaRestWebServiceImpl ssaRestWebServiceImpl;

    @Mock
    private SsaCreateAction ssaCreateAction;

    @Mock
    private SsaGetAction ssaGetAction;

    @Mock
    private SsaValidateAction ssaValidateAction;

    @Mock
    private SsaRevokeAction ssaRevokeAction;

    @Test
    public void create_validParams_validResponse() {
        when(ssaCreateAction.create(anyString(), any())).thenReturn(mock(Response.class));

        Response response = ssaRestWebServiceImpl.create("test request", mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");
        verify(ssaCreateAction).create(anyString(), any());
        verifyNoMoreInteractions(ssaCreateAction);
    }

    @Test
    public void get_validParams_validResponse() {
        when(ssaGetAction.get(anyString(), any(), any())).thenReturn(mock(Response.class));

        Response response = ssaRestWebServiceImpl.get("testJti", "org-id-test", mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");
        verify(ssaGetAction).get(anyString(), any(), any());
        verifyNoMoreInteractions(ssaGetAction);
    }

    @Test
    public void validate_validParams_validResponse() {
        when(ssaValidateAction.validate(anyString())).thenReturn(mock(Response.class));

        Response response = ssaRestWebServiceImpl.validate("testJti");
        assertNotNull(response, "response is null");
        verify(ssaValidateAction).validate(anyString());
        verifyNoMoreInteractions(ssaValidateAction);
    }

    @Test
    public void revoke_validParams_validResponse() {
        when(ssaRevokeAction.revoke(anyString(), any(), any())).thenReturn(mock(Response.class));

        Response response = ssaRestWebServiceImpl.revoke("testJti", "org-id-test", mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");
        verify(ssaRevokeAction).revoke(anyString(), any(), any());
        verifyNoMoreInteractions(ssaRevokeAction);
    }
}