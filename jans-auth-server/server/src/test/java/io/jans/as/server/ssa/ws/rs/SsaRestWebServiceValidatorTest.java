package io.jans.as.server.ssa.ws.rs;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.as.client.ssa.create.SsaCreateRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.error.DefaultErrorResponse;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ScopeService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

    @Mock
    private SsaService ssaService;

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

    @Test
    public void getValidSsaByJti_validJti_validSsa() {
        String jti = "test-jti";
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(Date.from(ZonedDateTime.now().plusHours(24).toInstant()));
        ssa.setState(SsaState.ACTIVE);
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);

        Ssa result = ssaRestWebServiceValidator.getValidSsaByJti(jti);
        assertNotNull(result, "ssa is null");
        verifyNoInteractions(log);
    }

    @Test
    public void getValidSsaByJti_ssaNull_400Status() {
        String jti = "test-jti";
        when(ssaService.findSsaByJti(jti)).thenReturn(null);
        DefaultErrorResponse entityError = new DefaultErrorResponse();
        entityError.setType(SsaErrorResponseType.INVALID_JTI);
        entityError.setErrorCode(SsaErrorResponseType.INVALID_JTI.getParameter());
        entityError.setReason("Invalid JTI or not exists");
        WebApplicationException webApplicationException = new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(entityError.toJSonString())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(any(), any(), any())).thenReturn(webApplicationException);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.getValidSsaByJti(jti));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertNotNull(ex.getResponse().getEntity());
        JSONObject entityJson = new JSONObject(ex.getResponse().getEntity().toString());
        assertTrue(entityJson.has("error"));
        assertEquals(entityJson.getString("error"), "invalid_jti");
        assertTrue(entityJson.has("reason"));
        assertEquals(entityJson.getString("reason"), "Invalid JTI or not exists");

        verify(log).warn(contains("is null or status"), eq(jti));
        verify(errorResponseFactory).createWebApplicationException(eq(Response.Status.BAD_REQUEST), eq(SsaErrorResponseType.INVALID_JTI), eq("Invalid JTI or not exists"));
    }

    @Test
    public void getValidSsaByJti_ssaExpired_403Status() {
        String jti = "test-jti";
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(Date.from(ZonedDateTime.now().minusHours(24).toInstant()));
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);
        DefaultErrorResponse entityError = new DefaultErrorResponse();
        entityError.setType(SsaErrorResponseType.INVALID_JTI);
        entityError.setErrorCode(SsaErrorResponseType.INVALID_JTI.getParameter());
        entityError.setReason("Invalid JTI or not exists");
        WebApplicationException webApplicationException = new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(entityError.toJSonString())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(any(), any(), any())).thenReturn(webApplicationException);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.getValidSsaByJti(jti));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertNotNull(ex.getResponse().getEntity());
        JSONObject entityJson = new JSONObject(ex.getResponse().getEntity().toString());
        assertTrue(entityJson.has("error"));
        assertEquals(entityJson.getString("error"), "invalid_jti");
        assertTrue(entityJson.has("reason"));
        assertEquals(entityJson.getString("reason"), "Invalid JTI or not exists");

        verify(log).warn(contains("is null or status"), eq(jti));
        verify(errorResponseFactory).createWebApplicationException(eq(Response.Status.BAD_REQUEST), eq(SsaErrorResponseType.INVALID_JTI), eq("Invalid JTI or not exists"));
    }

    @Test
    public void getValidSsaByJti_ssaWithUsedStatus_403Status() {
        String jti = "test-jti";
        Ssa ssa = new Ssa();
        ssa.setExpirationDate(Date.from(ZonedDateTime.now().plusHours(24).toInstant()));
        ssa.setState(SsaState.USED);
        when(ssaService.findSsaByJti(jti)).thenReturn(ssa);
        DefaultErrorResponse entityError = new DefaultErrorResponse();
        entityError.setType(SsaErrorResponseType.INVALID_JTI);
        entityError.setErrorCode(SsaErrorResponseType.INVALID_JTI.getParameter());
        entityError.setReason("Invalid JTI or not exists");
        WebApplicationException webApplicationException = new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(entityError.toJSonString())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        when(errorResponseFactory.createWebApplicationException(any(), any(), any())).thenReturn(webApplicationException);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.getValidSsaByJti(jti));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertNotNull(ex.getResponse().getEntity());
        JSONObject entityJson = new JSONObject(ex.getResponse().getEntity().toString());
        assertTrue(entityJson.has("error"));
        assertEquals(entityJson.getString("error"), "invalid_jti");
        assertTrue(entityJson.has("reason"));
        assertEquals(entityJson.getString("reason"), "Invalid JTI or not exists");

        verify(log).warn(contains("is null or status"), eq(jti));
        verify(errorResponseFactory).createWebApplicationException(eq(Response.Status.BAD_REQUEST), eq(SsaErrorResponseType.INVALID_JTI), eq("Invalid JTI or not exists"));
    }

    @Test
    void validateSsaCreateRequest_happyPath_success() {
        SsaCreateRequest ssaCreateRequest = new SsaCreateRequest();
        ssaCreateRequest.setLifetime(86400);

        ssaRestWebServiceValidator.validateSsaCreateRequest(ssaCreateRequest);
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void validateSsaCreateRequest_ifLifetimeIsNull_success() {
        SsaCreateRequest ssaCreateRequest = new SsaCreateRequest();
        ssaCreateRequest.setLifetime(null);

        ssaRestWebServiceValidator.validateSsaCreateRequest(ssaCreateRequest);
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void validateSsaCreateRequest_ifLifetimeIsZero_webApplicationException() {
        SsaCreateRequest ssaCreateRequest = new SsaCreateRequest();
        ssaCreateRequest.setLifetime(0);
        WebApplicationException webApplicationException = new WebApplicationException("Test exception");
        when(errorResponseFactory.createWebApplicationException(any(), any(), anyString())).thenThrow(webApplicationException);

        WebApplicationException ex = expectThrows(WebApplicationException.class, () -> ssaRestWebServiceValidator.validateSsaCreateRequest(ssaCreateRequest));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Test exception");
        verify(log).warn(eq("SSA Metadata validation: 'lifetime' cannot be 0 or negative"));
        verify(errorResponseFactory).createWebApplicationException(eq(Response.Status.BAD_REQUEST), eq(SsaErrorResponseType.INVALID_SSA_METADATA), eq("Invalid SSA Metadata"));
    }
}
