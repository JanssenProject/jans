/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.model.status.StatsData;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
    
    private StatsData statsData;

    public Conf findConf() {
        final String dn = configurationFactory.getAuthConfigurationDn();
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

    public StatsData getStatsData() {
        return statsData;
    }

    public void setStatsData(StatsData statsData) {
        this.statsData = statsData;
    }    
  
}
