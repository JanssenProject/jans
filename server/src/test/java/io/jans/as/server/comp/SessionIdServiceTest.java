/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import io.jans.as.common.service.common.UserService;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.model.common.SessionIdState;
import io.jans.as.server.service.SessionIdService;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public class SessionIdServiceTest extends BaseComponentTest {

    @Inject
    private SessionIdService service;

    @Inject
    private UserService userService;

    private SessionId generateSession(String userInum) {
        String userDn = userService.getDnForUser(userInum);
        return service.generateUnauthenticatedSessionId(userDn, new Date(), SessionIdState.UNAUTHENTICATED, new HashMap<>(), true);
    }

    @Parameters({"userInum"})
    @Test
    public void statePersistence(String userInum) {
        String userDn = userService.getDnForUser(userInum);
        SessionId newId = service.generateAuthenticatedSessionId(null, userDn);

        Assert.assertEquals(newId.getState(), SessionIdState.AUTHENTICATED);

        Map<String, String> sessionAttributes = new HashMap<String, String>();
        sessionAttributes.put("k1", "v1");
        newId.setSessionAttributes(sessionAttributes);

        service.updateSessionId(newId);

        final SessionId fresh = service.getSessionId(newId.getId());
        Assert.assertEquals(fresh.getState(), SessionIdState.AUTHENTICATED);
        Assert.assertTrue(fresh.getSessionAttributes().containsKey("k1"));
        Assert.assertTrue(fresh.getSessionAttributes().containsValue("v1"));
    }

    @Parameters({"userInum"})
    @Test
    public void testUpdateLastUsedDate(String userInum) {
        SessionId sessionId = generateSession(userInum);
        final SessionId fromLdap1 = service.getSessionId(sessionId.getId());
        final Date createdDate = sessionId.getLastUsedAt();
        System.out.println("Created date = " + createdDate);
        Assert.assertEquals(createdDate, fromLdap1.getLastUsedAt());

        sleepSeconds(1);
        service.updateSessionId(sessionId);

        final SessionId fromLdap2 = service.getSessionId(sessionId.getId());
        System.out.println("Updated date = " + fromLdap2.getLastUsedAt());
        Assert.assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
    }

    @Parameters({"userInum"})
    @Test
    public void testUpdateAttributes(String userInum) {
        SessionId sessionId = generateSession(userInum);
        final String clientId = "testClientId";
        final SessionId fromLdap1 = service.getSessionId(sessionId.getId());
        final Date createdDate = sessionId.getLastUsedAt();
        assertEquals(createdDate, fromLdap1.getLastUsedAt());
        assertFalse(fromLdap1.isPermissionGrantedForClient(clientId));

        sleepSeconds(1);
        sessionId.setAuthenticationTime(new Date());
        sessionId.addPermission(clientId, true);
        service.updateSessionId(sessionId);

        final SessionId fromLdap2 = service.getSessionId(sessionId.getId());
        assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
        assertNotNull(fromLdap2.getAuthenticationTime());
        assertTrue(fromLdap2.isPermissionGrantedForClient(clientId));
    }

}
