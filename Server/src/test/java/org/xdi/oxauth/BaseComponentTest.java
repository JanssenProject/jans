/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.web.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xdi.oxauth.service.AppInitializer;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/10/2012
 */

public abstract class BaseComponentTest extends BaseTest {
    @Before
    @Override
    public void begin() {
        Lifecycle.beginCall();
        super.begin();
    }

    @After
    @Override
    public void end() {
        Session.instance().invalidate();
        Lifecycle.endCall();
        super.end();
    }

    @BeforeClass
    public void setupClass() throws Exception {
        super.setupClass();
        Lifecycle.beginCall();
        beforeClass();
        Lifecycle.endCall();
    }

    public LdapEntryManager getLdapManager() {
        return (LdapEntryManager) Component.getInstance(AppInitializer.LDAP_ENTRY_MANAGER_NAME);
    }

    @AfterClass
    public void cleanupClass() throws Exception {
        Lifecycle.beginCall();
        afterClass();
        Lifecycle.endCall();
        super.cleanupClass();
    }

    public abstract void beforeClass();

    public abstract void afterClass();

    public static void sleepSeconds(int p_seconds) {
        try {
            Thread.sleep(p_seconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
