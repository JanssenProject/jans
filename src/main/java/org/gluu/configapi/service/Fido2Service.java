package org.gluu.configapi.service;

import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.configapi.configuration.ConfigurationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
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
            String configurationDn = configurationFactory.getBaseConfiguration().getString("fido2_ConfigurationEntryDN");
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
