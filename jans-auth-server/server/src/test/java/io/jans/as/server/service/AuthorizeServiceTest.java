/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.auth.Authenticator;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.security.Identity;
import io.jans.jsf2.message.FacesMessages;
import io.jans.jsf2.service.FacesService;
import io.jans.service.cdi.util.CdiUtil;
import jakarta.faces.context.ExternalContext;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizeServiceTest {

    @Spy
    @InjectMocks
    private AuthorizeService authorizeService;

    @Mock
    private Logger log;

    @Mock
    private ClientService clientService;

    @Mock
    private ClientIdMetadataService clientIdMetadataService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private CookieService cookieService;

    @Mock
    private ClientAuthorizationsService clientAuthorizationsService;

    @Mock
    private Identity identity;

    @Mock
    private Authenticator authenticator;

    @Mock
    private FacesService facesService;

    @Mock
    private FacesMessages facesMessages;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private RequestParameterService requestParameterService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Test
    public void permissionGranted_whenCimdClient_shouldResolveViaClientIdMetadataService() throws Exception {
        String cimdClientId = "https://rp.example.org/client-metadata.json";

        SessionId session = newSessionWithClientId(cimdClientId);

        User user = new User();
        user.setAttribute("inum", "user-inum", true);
        when(sessionIdService.getUser(session)).thenReturn(user);

        Client client = new Client();
        client.setClientId(cimdClientId);
        when(clientIdMetadataService.resolveClient(cimdClientId)).thenReturn(client);

        when(requestParameterService.getAllowedParameters(anyMap())).thenReturn(new HashMap<>());
        when(requestParameterService.parametersAsString(anyMap())).thenReturn("");
        when(httpServletRequest.getContextPath()).thenReturn("");

        authorizeService.permissionGranted(httpServletRequest, session);

        verify(clientIdMetadataService).resolveClient(cimdClientId);
        verify(clientService, never()).getClient(anyString());
        verify(authorizeService, never()).permissionDenied(any());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(facesService).redirectToExternalURL(redirectCaptor.capture());
        assertTrue(redirectCaptor.getValue().contains("/restv1/authorize?"));
    }

    @Test
    public void permissionGranted_whenClientNotFound_shouldDenyPermission() {
        String cimdClientId = "https://rp.example.org/client-metadata.json";

        SessionId session = newSessionWithClientId(cimdClientId);

        User user = new User();
        when(sessionIdService.getUser(session)).thenReturn(user);
        when(clientIdMetadataService.resolveClient(cimdClientId)).thenReturn(null);
        doNothing().when(authorizeService).permissionDenied(session);

        authorizeService.permissionGranted(httpServletRequest, session);

        verify(authorizeService).permissionDenied(session);
        verify(facesService, never()).redirectToExternalURL(anyString());
    }

    @Test
    public void permissionDenied_whenResponseModeJwtAndClientAlreadyResolved_shouldNotReResolveClient() {
        String cimdClientId = "https://rp.example.org/client-metadata.json";
        SessionId session = newJarmSessionWithClientId(cimdClientId);
        when(requestParameterService.getAllowedParameters(anyMap())).thenReturn(new HashMap<>());

        Client client = new Client();
        client.setClientId(cimdClientId);
        client.getAttributes().setAuthorizationSignedResponseAlg("RS256");

        ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);
        when(configurationFactory.getAppConfiguration()).thenReturn(appConfiguration);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ConfigurationFactory.class)).thenReturn(configurationFactory);

            authorizeService.permissionDenied(session, client);
        }

        verify(clientIdMetadataService, never()).resolveClient(anyString());
        verify(facesService, never()).redirect(anyString());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(facesService).redirectToExternalURL(redirectCaptor.capture());
        String redirectUrl = redirectCaptor.getValue();
        assertTrue(redirectUrl.startsWith("https://rp.example.org/cb"), "Expected redirect to target the client's redirect_uri but was: " + redirectUrl);
        assertTrue(redirectUrl.contains("response="), "Expected redirect to contain a signed JARM 'response' parameter but was: " + redirectUrl);
    }

    @Test
    public void permissionDenied_whenResponseModeJwtAndNoClientProvided_shouldFallBackToResolveClient() {
        String cimdClientId = "https://rp.example.org/client-metadata.json";
        SessionId session = newJarmSessionWithClientId(cimdClientId);
        when(requestParameterService.getAllowedParameters(anyMap())).thenReturn(new HashMap<>());

        Client client = new Client();
        client.setClientId(cimdClientId);
        client.getAttributes().setAuthorizationSignedResponseAlg("RS256");
        when(clientIdMetadataService.resolveClient(cimdClientId)).thenReturn(client);

        ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);
        when(configurationFactory.getAppConfiguration()).thenReturn(appConfiguration);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ConfigurationFactory.class)).thenReturn(configurationFactory);

            authorizeService.permissionDenied(session, null);
        }

        verify(clientIdMetadataService).resolveClient(cimdClientId);
        verify(facesService, never()).redirect(anyString());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(facesService).redirectToExternalURL(redirectCaptor.capture());
        String redirectUrl = redirectCaptor.getValue();
        assertTrue(redirectUrl.startsWith("https://rp.example.org/cb"), "Expected redirect to target the client's redirect_uri but was: " + redirectUrl);
        assertTrue(redirectUrl.contains("response="), "Expected redirect to contain a signed JARM 'response' parameter but was: " + redirectUrl);
    }

    @Test
    public void permissionDenied_whenResponseModeJwtAndClientCannotBeResolved_shouldFallBackToPlainRedirect() {
        String cimdClientId = "https://rp.example.org/client-metadata.json";
        SessionId session = newJarmSessionWithClientId(cimdClientId);
        when(requestParameterService.getAllowedParameters(anyMap())).thenReturn(new HashMap<>());

        when(clientIdMetadataService.resolveClient(cimdClientId)).thenReturn(null);

        authorizeService.permissionDenied(session, null);

        verify(clientIdMetadataService).resolveClient(cimdClientId);
        verify(facesService).redirectToExternalURL(anyString());
        verify(facesService, never()).redirect(anyString());
    }

    private static SessionId newJarmSessionWithClientId(String clientId) {
        SessionId session = new SessionId();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(AuthorizeRequestParam.CLIENT_ID, clientId);
        attributes.put(AuthorizeRequestParam.REDIRECT_URI, "https://rp.example.org/cb");
        attributes.put(AuthorizeRequestParam.STATE, "state123");
        attributes.put(AuthorizeRequestParam.RESPONSE_MODE, "jwt");
        attributes.put(AuthorizeRequestParam.RESPONSE_TYPE, "code");
        session.setSessionAttributes(attributes);
        return session;
    }

    private static SessionId newSessionWithClientId(String clientId) {
        SessionId session = new SessionId();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(AuthorizeRequestParam.CLIENT_ID, clientId);
        attributes.put(AuthorizeRequestParam.SCOPE, "openid");
        attributes.put(AuthorizeRequestParam.RESPONSE_TYPE, "code");
        session.setSessionAttributes(attributes);
        session.setUserDn("user-dn");
        return session;
    }
}
