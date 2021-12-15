/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.orm.PersistenceEntryManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ConfService {
    
    private static String DN = "ou=jans-auth,ou=configuration,o=jans";
    
    @Inject
    private Logger logger;
    
    @Inject
    private PersistenceEntryManager persistenceEntryManager;


    @Inject
    ConfigurationService configurationService;

    public Conf findConf() {
        logger.error("\n\n ConfService::findConf() - Entry \n\n");
        return persistenceEntryManager.find(DN, Conf.class, null);
    }
    

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamic();
    }

}
