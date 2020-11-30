/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.common.service.common.UserService;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.model.common.SessionIdState;
import io.jans.as.server.service.SessionIdService;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public class SessionIdServiceTest extends BaseComponentTest {

    @Inject
    private SessionIdService m_service;

    @Inject
    private UserService userService;

    private SessionId generateSession(String userInum) {
        String userDn = userService.getDnForUser(userInum);
        return m_service.generateUnauthenticatedSessionId(userDn, new Date(), SessionIdState.UNAUTHENTICATED,
                new HashMap<String, String>(), true);
    }

    @Parameters({"userInum"})
    @Test
    public void statePersistence(String userInum) {
        String userDn = userService.getDnForUser(userInum);
        SessionId newId = m_service.generateAuthenticatedSessionId(null, userDn);

        Assert.assertEquals(newId.getState(), SessionIdState.AUTHENTICATED);

        Map<String, String> sessionAttributes = new HashMap<String, String>();
        sessionAttributes.put("k1", "v1");
        newId.setSessionAttributes(sessionAttributes);

        m_service.updateSessionId(newId);

        final SessionId fresh = m_service.getSessionId(newId.getId());
        Assert.assertEquals(fresh.getState(), SessionIdState.AUTHENTICATED);
        Assert.assertTrue(fresh.getSessionAttributes().containsKey("k1"));
        Assert.assertTrue(fresh.getSessionAttributes().containsValue("v1"));
    }

    @Parameters({"userInum"})
    @Test
    public void testUpdateLastUsedDate(String userInum) {
        SessionId m_sessionId = generateSession(userInum);
        final SessionId fromLdap1 = m_service.getSessionId(m_sessionId.getId());
        final Date createdDate = m_sessionId.getLastUsedAt();
        System.out.println("Created date = " + createdDate);
        Assert.assertEquals(createdDate, fromLdap1.getLastUsedAt());

        sleepSeconds(1);
        m_service.updateSessionId(m_sessionId);

        final SessionId fromLdap2 = m_service.getSessionId(m_sessionId.getId());
        System.out.println("Updated date = " + fromLdap2.getLastUsedAt());
        Assert.assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
    }

    @Parameters({"userInum"})
    @Test
    public void testUpdateAttributes(String userInum) {
        SessionId m_sessionId = generateSession(userInum);
        final String clientId = "testClientId";
        final SessionId fromLdap1 = m_service.getSessionId(m_sessionId.getId());
        final Date createdDate = m_sessionId.getLastUsedAt();
        assertEquals(createdDate, fromLdap1.getLastUsedAt());
        assertFalse(fromLdap1.isPermissionGrantedForClient(clientId));

        sleepSeconds(1);
        m_sessionId.setAuthenticationTime(new Date());
        m_sessionId.addPermission(clientId, true);
        m_service.updateSessionId(m_sessionId);

        final SessionId fromLdap2 = m_service.getSessionId(m_sessionId.getId());
        assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
        assertNotNull(fromLdap2.getAuthenticationTime());
        assertTrue(fromLdap2.isPermissionGrantedForClient(clientId));
    }

}
