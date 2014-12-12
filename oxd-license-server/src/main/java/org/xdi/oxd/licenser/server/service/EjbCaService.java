package org.xdi.oxd.licenser.server.service;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.inject.Inject;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.js.Configuration;
import org.xdi.oxd.license.client.js.LdapLicenseId;

import java.io.File;

/**
 * @author yuriyz on 12/12/2014.
 */
public class EjbCaService {

    private static final Logger LOG = LoggerFactory.getLogger(LdapStructureChecker.class);

    private static final String TRUST_STORE_NAME = "LicenseServer_TrustStore.jks";
    private static final String KEY_STORE_NAME = "LicenseServer_KeyStore.jks";

    public static final String CANT_FIND_STORES_ERROR = "Failed to find out path keystore and truststore to setup connection to EjbCa. " +
            "It's expected to have keyStore : <catalina.home>/conf/" + KEY_STORE_NAME +
            ", and trustStore: <catalina.home>/conf/" + TRUST_STORE_NAME +
            ". Otherwise please set custom path via -Dgluu.ejbca.storePath= java system property (E.g. -Dgluu.ejbca.storePath=/home/yuriyz)";

    @Inject
    LdapEntryManager ldapEntryManager;
    @Inject
    Configuration conf;

    public void createUser(LdapLicenseId ldapLicenseId) {

    }

    public void setupKeyStoreAndTrustStore() {
        System.setProperty("javax.net.ssl.trustStore", getPathToStore() + File.separator + TRUST_STORE_NAME);
        System.setProperty("javax.net.ssl.trustStorePassword", "secret");

        System.setProperty("javax.net.ssl.keyStore", getPathToStore() + File.separator + KEY_STORE_NAME);
        System.setProperty("javax.net.ssl.keyStorePassword", "secret");
    }

    private static String getPathToStore() {
        String tomcatHome =  System.getProperty("catalina.home") + File.separator + "conf" + File.separator;
        if (pathExists(tomcatHome)) {
            return tomcatHome;
        }
        String pathToStore = System.getProperty("gluu.ejbca.storePath");
        if (pathExists(pathToStore)) {
            return pathToStore;
        }
        LOG.error(CANT_FIND_STORES_ERROR);
        throw new RuntimeException(CANT_FIND_STORES_ERROR);
    }

    private static boolean pathExists(String tomcatHome) {
        return new File(tomcatHome + KEY_STORE_NAME).exists();
    }
}
