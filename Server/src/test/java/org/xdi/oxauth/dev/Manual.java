/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.dev;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.util.properties.FileConfiguration;
import org.xdi.util.security.PropertiesDecrypter;
import org.xdi.util.security.StringEncrypter;

/**
 * Test for manual run. Used for development purpose ONLY. Must not be run in suite.
 * ATTENTION : To make life easier must not have dependency on embedded server.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/07/2012
 */

public class Manual {

    public static String LDAP_CONF_FILE_NAME = "oxauth-ldap.properties";
    public static final String CONF_FOLDER = "conf";

    private static final String LDAP_FILE_PATH = CONF_FOLDER + File.separator + LDAP_CONF_FILE_NAME;

    public static LdapEntryManager MANAGER = null;

    @BeforeClass
    public void init() {
        final FileConfiguration fileConfiguration = new FileConfiguration(LDAP_FILE_PATH);
        final Properties props = PropertiesDecrypter.decryptProperties(fileConfiguration.getProperties(), "passoword");
        final LDAPConnectionProvider connectionProvider = new LDAPConnectionProvider(props);
        MANAGER = new LdapEntryManager(new OperationsFacade(connectionProvider));
    }

    @AfterClass
    public void destroy() {
        MANAGER.getLdapOperationService().getConnectionPool().close();
    }

    @Test
    public void addGroupsToClient() throws StringEncrypter.EncryptionException {
        Client c = new Client();
        c.setDn("inum=@!0000!0008!7652.0000,ou=clients,o=@!1111,o=gluu");
        c.setClientId("@!0000!0008!7652.0000"); // inum
        c.setClientName("web");
        c.setApplicationType("web");
        c.setClientSecretExpiresAt(new Date());
        c.setClientSecret("00000000-0000-0000-0000-097337e87435");
        c.setUserGroups(new String[]{
                "inum=@!1111!0003!D9B4,ou=groups,o=@!1111,o=gluu",
                "inum=@!1111!0003!A3F4,ou=groups,o=@!1111,o=gluu"
        });
        MANAGER.persist(c);
    }

    @Test
    public void getGroupsFromClient() {
        final Client client = MANAGER.find(Client.class, "inum=@!0000!0008!7652.0000,ou=clients,o=@!1111,o=gluu");
        System.out.println(client);
    }
}
