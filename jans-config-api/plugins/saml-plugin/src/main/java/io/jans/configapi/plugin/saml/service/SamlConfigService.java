package io.jans.configapi.plugin.saml.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.saml.configuration.SamlConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.config.SamlConf;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class SamlConfigService {
    
    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    SamlConfigurationFactory samlConfigurationFactory;

    public SamlConf findSamlConf() {
        final String dn = samlConfigurationFactory.getSamlConfigurationDn();
        return persistenceManager.find(dn, SamlConf.class, null);
    }

    public void mergeSamlConfig(SamlConf samlConf) {
        samlConf.setRevision(samlConf.getRevision() + 1);
        persistenceManager.merge(samlConf);
    }

    public SamlAppConfiguration find() {
        final SamlConf samlConf = findSamlConf();
        return samlConf.getDynamicConf();
    }


}
