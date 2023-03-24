/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.fido2.model.conf.Conf;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Fido2Service {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public Conf findConf() {
        try {
            String configurationDn = configurationFactory.getBaseConfiguration()
                    .getString("fido2_ConfigurationEntryDN");
            return persistenceManager.find(Conf.class, configurationDn);
        } catch (BasePersistenceException var3) {
            logger.error("Failed to load Fido2 configuration from LDAP");
            return null;
        }
    }

    public AppConfiguration find() {
        final Conf conf = findConf();
        return conf.getDynamicConf();
    }

    public void mergeConf(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.merge(conf);
    }

    public void merge(AppConfiguration fido2ConfigJson) {
        Conf conf = this.findConf();
        conf.setDynamicConf(fido2ConfigJson);
        mergeConf(conf);
    }
}
