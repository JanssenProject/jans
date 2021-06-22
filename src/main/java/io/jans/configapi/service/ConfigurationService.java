/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ConfigurationService {

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public Conf findConf() {
        final String dn = configurationFactory.getConfigurationDn();
        return persistenceManager.find(dn, Conf.class, null);
    }

    public void merge(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public void merge(GluuConfiguration conf) {
        persistenceManager.merge(conf);
    }

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamic();
    }

    public GluuConfiguration findGluuConfiguration() {
        String configurationDn = findConf().getStatics().getBaseDn().getConfiguration();
        if (StringHelper.isEmpty(configurationDn)) {
            return null;
        }
        return persistenceManager.find(GluuConfiguration.class, configurationDn);
    }
    
    public String getPersistenceType() {
        return configurationFactory.getBaseConfiguration().getString("persistence.type");
    }
}
