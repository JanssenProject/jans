/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.example;

import io.jans.cacherefresh.service.config.ConfigurationFactory;
import io.jans.cacherefresh.timer.CacheRefreshTimer;
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

        //gluucacherefresh
        //GluuConfiguration GluuConfiguration = new GluuConfiguration();
        CacheRefreshTimer cacheRefreshTimer = new CacheRefreshTimer();
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
