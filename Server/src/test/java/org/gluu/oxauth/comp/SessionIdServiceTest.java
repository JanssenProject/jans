/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.comp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gluu.oxauth.BaseComponentTest;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.common.SessionIdState;
import org.gluu.oxauth.service.SessionIdService;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.service.UserService;

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

        final SessionId fresh = m_service.getSessionById(newId.getId());
        Assert.assertEquals(fresh.getState(), SessionIdState.AUTHENTICATED);
        Assert.assertTrue(fresh.getSessionAttributes().containsKey("k1"));
        Assert.assertTrue(fresh.getSessionAttributes().containsValue("v1"));
    }

    @Parameters({"userInum"})
    @Test
    public void testUpdateLastUsedDate(String userInum) {
        SessionId m_sessionId = generateSession(userInum);
        final SessionId fromLdap1 = m_service.getSessionById(m_sessionId.getId());
        final Date createdDate = m_sessionId.getLastUsedAt();
        System.out.println("Created date = " + createdDate);
        Assert.assertEquals(createdDate, fromLdap1.getLastUsedAt());

        sleepSeconds(1);
        m_service.updateSessionId(m_sessionId);

        final SessionId fromLdap2 = m_service.getSessionById(m_sessionId.getId());
        System.out.println("Updated date = " + fromLdap2.getLastUsedAt());
        Assert.assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
    }

    @Parameters({"userInum"})
    @Test
    public void testUpdateAttributes(String userInum) {
        SessionId m_sessionId = generateSession(userInum);
        final String clientId = "testClientId";
        final SessionId fromLdap1 = m_service.getSessionById(m_sessionId.getId());
        final Date createdDate = m_sessionId.getLastUsedAt();
        assertEquals(createdDate, fromLdap1.getLastUsedAt());
        assertFalse(fromLdap1.isPermissionGrantedForClient(clientId));

        sleepSeconds(1);
        m_sessionId.setAuthenticationTime(new Date());
        m_sessionId.addPermission(clientId, true);
        m_service.updateSessionId(m_sessionId);

        final SessionId fromLdap2 = m_service.getSessionById(m_sessionId.getId());
        assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
        assertNotNull(fromLdap2.getAuthenticationTime());
        assertTrue(fromLdap2.isPermissionGrantedForClient(clientId));
    }

}
