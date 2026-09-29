/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.ClientIdMetadataService;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.SessionIdService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class ConsentGatheringSessionServiceTest {

    @InjectMocks
    private ConsentGatheringSessionService consentGatheringSessionService;

    @Mock
    private Logger log;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private ClientIdMetadataService clientIdMetadataService;

    @Mock
    private CookieService cookieService;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void getClient_whenCimdClientIdInSession_shouldResolveViaClientIdMetadataService() {
        String cimdClientId = "https://rp.example.org/client-metadata.json";
        SessionId session = new SessionId();
        consentGatheringSessionService.setClientId(session, cimdClientId);

        Client cimdClient = new Client();
        cimdClient.setClientId(cimdClientId);
        when(clientIdMetadataService.resolveClient(cimdClientId)).thenReturn(cimdClient);

        Client result = consentGatheringSessionService.getClient(session);

        assertEquals(result, cimdClient);
        verify(clientIdMetadataService).resolveClient(cimdClientId);
    }

    @Test
    public void getClient_whenNoClientIdInSession_shouldReturnNullWithoutLookup() {
        SessionId session = new SessionId();

        Client result = consentGatheringSessionService.getClient(session);

        assertNull(result);
        verify(clientIdMetadataService, never()).resolveClient(org.mockito.ArgumentMatchers.anyString());
    }
}
