/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.conf;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.ApiConf;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class ConfigApiService {

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public ApiConf findApiConf() {
        final String dn = configurationFactory.getApiAppConfigurationDn();
        return persistenceManager.find(dn, ApiConf.class, null);
    }

    public void merge(ApiConf apiConf) {
        apiConf.setRevision(apiConf.getRevision() + 1);
        persistenceManager.merge(apiConf);
    }

    public ApiAppConfiguration find() {
        final ApiConf apiConf = findApiConf();
        return apiConf.getDynamicConf();
    }

}
