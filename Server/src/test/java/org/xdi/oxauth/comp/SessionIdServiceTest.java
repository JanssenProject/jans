/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.service.SessionIdService;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 05/28/2013
 */

public class SessionIdServiceTest extends BaseComponentTest {

    private SessionId m_sessionId;
    private SessionIdService m_service;

    @Override
    public void beforeClass() {
        m_service = SessionIdService.instance();
        m_sessionId = m_service.generateSessionIdInteractive("dummyDn");
    }

    @Override
    public void afterClass() {
        if (m_sessionId != null) {
            getLdapManager().remove(m_sessionId);
        }
    }

    @Test
    public void testUpdateLastUsedDate() {
        final SessionId fromLdap1 = m_service.getSessionByDN(m_sessionId.getDn());
        final Date createdDate = m_sessionId.getLastUsedAt();
        System.out.println("Created date = " + createdDate);
        Assert.assertEquals(createdDate, fromLdap1.getLastUsedAt());

        sleepSeconds(1);
        m_service.updateSessionWithLastUsedDate(m_sessionId);

        final SessionId fromLdap2 = m_service.getSessionByDN(m_sessionId.getDn());
        System.out.println("Updated date = " + fromLdap2.getLastUsedAt());
        Assert.assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
    }

    @Test
    public void testUpdateAttributes() {
        final String clientId = "testClientId";
        final SessionId fromLdap1 = m_service.getSessionByDN(m_sessionId.getDn());
        final Date createdDate = m_sessionId.getLastUsedAt();
        assertEquals(createdDate, fromLdap1.getLastUsedAt());
        assertNull(fromLdap1.getAuthenticationTime());
        assertFalse(fromLdap1.isPermissionGrantedForClient(clientId));

        sleepSeconds(1);
        m_sessionId.setAuthenticationTime(new Date());
        m_sessionId.addPermission(clientId, true);
        m_service.updateSessionWithLastUsedDate(m_sessionId);

        final SessionId fromLdap2 = m_service.getSessionByDN(m_sessionId.getDn());
        assertTrue(createdDate.before(fromLdap2.getLastUsedAt()));
        assertNotNull(fromLdap2.getAuthenticationTime());
        assertTrue(fromLdap2.isPermissionGrantedForClient(clientId));
    }


    @Test
    public void testOldSessionsIdentification() {
        sleepSeconds(2);

        Assert.assertTrue(m_service.getIdsOlderThan(1).contains(m_sessionId));
        Assert.assertTrue(!m_service.getIdsOlderThan(10).contains(m_sessionId));
    }
}
