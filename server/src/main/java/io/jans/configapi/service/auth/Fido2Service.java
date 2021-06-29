/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.config.oxtrust.DbApplicationConfiguration;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class Fido2Service {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public DbApplicationConfiguration find() {
        try {
            String configurationDn = configurationFactory.getBaseConfiguration()
                    .getString("fido2_ConfigurationEntryDN");
            return persistenceManager.find(DbApplicationConfiguration.class, configurationDn);
        } catch (BasePersistenceException var3) {
            logger.error("Failed to load Fido2 configuration from LDAP");
            return null;
        }
    }

    public void merge(String fido2ConfigJson) {
        DbApplicationConfiguration fido2Configuration = this.find();
        fido2Configuration.setDynamicConf(fido2ConfigJson);
        fido2Configuration.setRevision(fido2Configuration.getRevision() + 1L);
        persistenceManager.merge(fido2Configuration);
    }
}
