/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.test;

import io.jans.link.service.config.ConfigurationFactory;
import io.jans.link.timer.JansLinkTimer;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class LdapSample {


    private static final Logger LOG = LoggerFactory.getLogger(LdapSample.class);

    private LdapSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapEntryManagerSample entryManagerSample = new LdapEntryManagerSample();

        // Create LDAP entry manager
        PersistenceEntryManager entryManager = entryManagerSample.createLdapEntryManager();

        //LinkConfiguration linkConfiguration = new LinkConfiguration();
        JansLinkTimer cacheRefreshTimer = new JansLinkTimer();
        cacheRefreshTimer.setLdapEntryManager(entryManager);
        ConfigurationFactory configurationFactory = new ConfigurationFactory();
        //configurationFactory.setPersistenceEntryManagerInstance(entryManager);
        configurationFactory.create();
        cacheRefreshTimer.setConfigurationFactory(configurationFactory);
        /*ConfigurationService configurationService = new ConfigurationService();
        configurationService.setPersistenceEntryManager(entryManager);
        cacheRefreshTimer.setConfigurationService(configurationService);
        cacheRefreshTimer.processInt();*/


    }

}
