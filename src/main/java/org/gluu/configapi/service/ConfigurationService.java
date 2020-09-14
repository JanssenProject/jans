package org.gluu.configapi.service;

import org.gluu.configapi.configuration.ConfigurationFactory;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxtrust.service.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;

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
    Instance<PersistenceEntryManager> persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    public Conf find(String... returnAttributes) {
        final String dn = configurationFactory.getBaseConfiguration().getString("oxauth_ConfigurationEntryDN");
        return persistenceManager.get().find(dn, Conf.class, returnAttributes);
    }

    public void merge(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.get().merge(conf);
    }
}
