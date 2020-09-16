package org.gluu.configapi.service;

import org.gluu.configapi.configuration.ConfigurationFactory;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.service.common.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.configuration.GluuConfiguration;

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

    public Conf findConf() {
        final String dn = configurationFactory.getOxauthConfigurationDn();
        return persistenceManager.get().find(dn, Conf.class, null);
    }

    public void merge(Conf conf) {
        conf.setRevision(conf.getRevision() + 1);
        persistenceManager.get().merge(conf);
    }

    public void merge(GluuConfiguration conf) {
        persistenceManager.get().merge(conf);
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
        return persistenceManager.get().find(GluuConfiguration.class, configurationDn);
    }
}
