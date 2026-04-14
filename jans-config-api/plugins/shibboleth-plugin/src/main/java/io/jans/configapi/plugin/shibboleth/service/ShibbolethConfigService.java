package io.jans.configapi.plugin.shibboleth.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.shibboleth.configuration.ShibbolethPluginConfigurationFactory;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginAppConf;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginConfiguration;

import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ShibbolethConfigService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ShibbolethPluginConfigurationFactory shibbolethPluginConfigurationFactory;

    // Config handling methods
    public ShibbolethPluginAppConf findShibbolethPluginConfiguration() {
        final String dn = shibbolethPluginConfigurationFactory.getShibbolethPluginAppConfigurationDn();
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("Shibboleth Configuration DN is undefined!");
        }

        logger.info(" dn:{}", dn);
        ShibbolethPluginAppConf shibbolethPluginAppConf = persistenceManager.find(dn, ShibbolethPluginAppConf.class,
                null);
        logger.info(" shibbolethPluginAppConf:{}", shibbolethPluginAppConf);

        return shibbolethPluginAppConf;
    }

    public void mergeShibbolethPluginAppConf(ShibbolethPluginAppConf shibbolethPluginAppConf) {
        shibbolethPluginAppConf.setRevision(shibbolethPluginAppConf.getRevision() + 1);
        persistenceManager.merge(shibbolethPluginAppConf);
    }

    public ShibbolethPluginConfiguration find() {
        return getShibbolethPluginAppConf().getDynamicConf();
    }

    public String getSpMetadataDir() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String spMetadataDir = null;
        if (shibbolethPluginConfiguration != null) {
            spMetadataDir = shibbolethPluginConfiguration.getSpMetadataDir();
        }
        return spMetadataDir;
    }

    public String getSpMetadataFilePattern() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String spMetadataFilePattern = null;
        if (shibbolethPluginConfiguration != null) {
            spMetadataFilePattern = shibbolethPluginConfiguration.getSpMetadataFilePattern();
        }
        return spMetadataFilePattern;
    }

    public String getSpMetadataFile() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String spMetadataFile = null;
        if (shibbolethPluginConfiguration != null) {
            spMetadataFile = shibbolethPluginConfiguration.getSpMetadataFile();
        }
        return spMetadataFile;
    }

    // Utility methods
    private ShibbolethPluginAppConf getShibbolethPluginAppConf() {
        ShibbolethPluginAppConf shibbolethPluginAppConf = findShibbolethPluginConfiguration();
        if (shibbolethPluginAppConf == null) {
            throw new InvalidConfigurationException("ShibbolethPluginAppConf is undefined!");
        }
        logger.debug("  shibbolethPluginAppConf:{}, shibbolethPluginAppConf.getDynamicConf():{}",
                shibbolethPluginAppConf, shibbolethPluginAppConf.getDynamicConf());
        return shibbolethPluginAppConf;
    }
}
