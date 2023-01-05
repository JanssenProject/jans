package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ScopeService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaRestWebServiceValidatorTest {

    @InjectMocks
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Mock
    private Identity identity;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ScopeService scopeService;

    @Test
    public void getClientFromSession_sessionClient_validClient() {
        SessionClient sessionClient = new SessionClient();
        Client client = new Client();
        client.setClientId("test_id");
        sessionClient.setClient(client);
        doReturn(sessionClient).when(identity).getSessionClient();

        Client clientAux = ssaRestWebServiceValidator.getClientFromSession();
        assertNotNull(clientAux, "client is null");
        verify(log).debug(anyString(), anyString());
    }

    @Test
    public void getClientFromSession_sessionClientNull_invalidClientResponse() {
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());

        when(identity.getSessionClient()).thenReturn(null);
        when(errorResponseFactory.createBadRequestException(eq(SsaErrorResponseType.INVALID_CLIENT), anyString())).thenThrow(error);

        try {
            ssaRestWebServiceValidator.getClientFromSession();
        } catch (WebApplicationException e) {
            assertNotNull(e, "WebApplicationException is null");
            assertNotNull(e.getResponse(), "WebApplicationException Response is null");
        }
        verify(identity).getSessionClient();
        verifyNoInteractions(log);
    }

    @Test
    public void checkScopesPolicySingleScope_clientAndScopeContains_validScope() {
        String scope = "test_id";
        Client client = new Client();
        client.setScopes(new String[]{});
        when(scopeService.getScopeIdsByDns(anyList())).thenReturn(Collections.singletonList("test_id"));

        ssaRestWebServiceValidator.checkScopesPolicy(client, scope);
        verifyNoInteractions(errorResponseFactory);
    }

    @Test
    public void checkScopesPolicySingleScope_clientAndScopeNotContains_unauthorizedResponse() {
        String scope = "test_id";
        Client client = new Client();
        client.setScopes(new String[]{});
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(scopeService.getScopeIdsByDns(anyList())).thenReturn(Collections.singletonList("test_id_fail"));
        when(errorResponseFactory.createWebApplicationException(eq(Response.Status.UNAUTHORIZED), eq(SsaErrorResponseType.UNAUTHORIZED_CLIENT), anyString())).thenThrow(error);

        WebApplicationException wae = null;
        try {
            ssaRestWebServiceValidator.checkScopesPolicy(client, scope);
        } catch (WebApplicationException e) {
            wae = e;
        }
        assertNotNull(wae, "WebApplicationException is null");
        assertNotNull(wae.getResponse(), "WebApplicationException Response is null");
    }

    @Test
    public void checkScopesPolicyListScope_clientNull_unauthorizedResponse() {
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(eq(Response.Status.UNAUTHORIZED), eq(SsaErrorResponseType.UNAUTHORIZED_CLIENT), anyString())).thenThrow(error);

        Client client = null;
        List<String> scopeList = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());
        assertThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.checkScopesPolicy(client, scopeList));
        verify(errorResponseFactory).createWebApplicationException(any(), any(), anyString());
        verifyNoInteractions(scopeService);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    public void checkScopesPolicyListScope_scopeListNull_unauthorizedResponse() {
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(eq(Response.Status.UNAUTHORIZED), eq(SsaErrorResponseType.UNAUTHORIZED_CLIENT), anyString())).thenThrow(error);

        Client client = new Client();
        List<String> scopeList = null;
        assertThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.checkScopesPolicy(client, scopeList));
        verify(errorResponseFactory).createWebApplicationException(any(), any(), anyString());
        verifyNoInteractions(scopeService);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    public void checkScopesPolicyListScope_scopeListEmpty_unauthorizedResponse() {
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(eq(Response.Status.UNAUTHORIZED), eq(SsaErrorResponseType.UNAUTHORIZED_CLIENT), anyString())).thenThrow(error);

        Client client = new Client();
        List<String> scopeList = new ArrayList<>();
        assertThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.checkScopesPolicy(client, scopeList));
        verify(errorResponseFactory).createWebApplicationException(any(), any(), anyString());
        verifyNoInteractions(scopeService);
        verifyNoMoreInteractions(errorResponseFactory);
    }

    @Test
    public void checkScopesPolicyListScope_clientAndScopeAdmin_valid() {
        String scope = SsaScopeType.SSA_ADMIN.getValue();
        Client client = new Client();
        client.setScopes(new String[]{});
        when(scopeService.getScopeIdsByDns(anyList())).thenReturn(Collections.singletonList(scope));

        List<String> scopeList = new ArrayList<>();
        scopeList.add(SsaScopeType.SSA_ADMIN.getValue());

        ssaRestWebServiceValidator.checkScopesPolicy(client, scopeList);
        verify(scopeService).getScopeIdsByDns(any());
        verifyNoInteractions(errorResponseFactory);
    }

    @Test
    public void checkScopesPolicyListScope_clientAndScopeNotContains_unauthorizedResponse() {
        String scope = SsaScopeType.SSA_ADMIN.getValue();
        Client client = new Client();
        client.setScopes(new String[]{});
        when(scopeService.getScopeIdsByDns(anyList())).thenReturn(Collections.singletonList(scope));
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(eq(Response.Status.UNAUTHORIZED), eq(SsaErrorResponseType.UNAUTHORIZED_CLIENT), anyString())).thenThrow(error);

        List<String> scopeList = new ArrayList<>();
        scopeList.add(SsaScopeType.SSA_PORTAL.getValue());
        assertThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.checkScopesPolicy(client, scopeList));
        verify(scopeService).getScopeIdsByDns(anyList());
        verify(errorResponseFactory).createWebApplicationException(any(), any(), anyString());
        verifyNoMoreInteractions(errorResponseFactory);
    }
}