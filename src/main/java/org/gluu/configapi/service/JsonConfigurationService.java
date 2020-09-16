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
public class JsonConfigurationService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public DbApplicationConfiguration loadFido2Configuration() {
        try {
            String configurationDn = configurationFactory.getBaseConfiguration().getString("fido2_ConfigurationEntryDN");
            DbApplicationConfiguration conf = persistenceManager.find(DbApplicationConfiguration.class, configurationDn);
            return conf;
        } catch (BasePersistenceException var3) {
            logger.error("Failed to load Fido2 configuration from LDAP");
            return null;
        }
    }

    public void saveFido2Configuration(String fido2ConfigJson) {
        DbApplicationConfiguration fido2Configuration = this.loadFido2Configuration();
        fido2Configuration.setDynamicConf(fido2ConfigJson);
        fido2Configuration.setRevision(fido2Configuration.getRevision() + 1L);
        persistenceManager.merge(fido2Configuration);
    }

}
