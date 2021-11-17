/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.orm.PersistenceEntryManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
public class ConfService {

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    public Conf findConf(String dn) {
        return persistenceManager.find(dn, Conf.class, null);
    }

    public void merge(GluuConfiguration conf) {
        persistenceManager.merge(conf);
    }

    public AppConfiguration find(String dn) {
        final Conf conf = findConf(dn);
        return conf.getDynamic();
    }

}
