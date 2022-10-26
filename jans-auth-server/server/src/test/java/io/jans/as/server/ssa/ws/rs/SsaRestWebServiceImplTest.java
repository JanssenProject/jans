package io.jans.as.server.ssa.ws.rs;

import io.jans.as.server.ssa.ws.rs.action.SsaCreateAction;
import io.jans.as.server.ssa.ws.rs.action.SsaGetAction;
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
    private SsaRestWebServiceImpl ssaRestWebService;

    @Mock
    private SsaCreateAction ssaCreateAction;

    @Mock
    private SsaGetAction ssaGetAction;

    @Test
    public void create_validParams_validResponse() {
        when(ssaCreateAction.create(anyString(), any())).thenReturn(mock(Response.class));
        Response response = ssaRestWebService.create("test request", mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");

        verify(ssaCreateAction).create(anyString(), any());
        verifyNoMoreInteractions(ssaCreateAction);
    }

    @Test
    public void get_validParams_validResponse() {
        when(ssaGetAction.get(anyBoolean(), anyString(), anyString(), any())).thenReturn(mock(Response.class));
        Response response = ssaRestWebService.get(false, "testJti", "testOrgId", mock(HttpServletRequest.class));
        assertNotNull(response, "response is null");

        verify(ssaGetAction).get(anyBoolean(), anyString(), anyString(), any());
        verifyNoMoreInteractions(ssaGetAction);
    }
}