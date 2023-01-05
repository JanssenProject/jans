/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ConfService {
    
    private static String dn = "ou=jans-auth,ou=configuration,o=jans";
    
    @Inject
    private Logger logger;
    
    @Inject
    private PersistenceEntryManager persistenceEntryManager;


    @Inject
    ConfigurationService configurationService;

    public Conf findConf() {
        logger.debug("\n\n ConfService::findConf() - Entry \n\n");
        return persistenceEntryManager.find(dn, Conf.class, null);
    }
    

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamic();
    }

}
