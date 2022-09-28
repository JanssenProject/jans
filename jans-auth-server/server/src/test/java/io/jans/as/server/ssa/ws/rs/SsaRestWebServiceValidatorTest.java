package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNotNull;

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
    public void validateClient_sessionClient_validClient() {
        SessionClient sessionClient = new SessionClient();
        Client client = new Client();
        client.setClientId("test_id");
        sessionClient.setClient(client);
        doReturn(sessionClient).when(identity).getSessionClient();

        Client clientAux = ssaRestWebServiceValidator.validateClient();
        assertNotNull(clientAux, "client is null");
        verify(log).debug(anyString(), anyString());
    }

    @Test
    public void validateClient_sessionClientNull_invalidClientResponse() {
        WebApplicationException error = new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Invalid client")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());

        when(identity.getSessionClient()).thenReturn(null);
        when(errorResponseFactory.createBadRequestException(eq(SsaErrorResponseType.INVALID_CLIENT), anyString())).thenThrow(error);

        try {
            ssaRestWebServiceValidator.validateClient();
        } catch (WebApplicationException e) {
            assertNotNull(e, "WebApplicationException is null");
            assertNotNull(e.getResponse(), "WebApplicationException Response is null");
        }
        verify(identity).getSessionClient();
        verifyNoInteractions(log);
    }

    @Test
    public void checkScopesPolicy_clientAndScopeConstains_validScope() {
        String scope = "test_id";
        Client client = new Client();
        client.setScopes(new String[]{});
        when(scopeService.getScopeIdsByDns(anyList())).thenReturn(Collections.singletonList("test_id"));

        ssaRestWebServiceValidator.checkScopesPolicy(client, scope);
        verifyNoInteractions(errorResponseFactory);
    }

    @Test
    public void checkScopesPolicy_clientAndScopeNotConstains_unauthorizedResponse() {
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

        try {
            ssaRestWebServiceValidator.checkScopesPolicy(client, scope);
        } catch (WebApplicationException e) {
            assertNotNull(e, "WebApplicationException is null");
            assertNotNull(e.getResponse(), "WebApplicationException Response is null");
        }
    }
}