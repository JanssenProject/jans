package io.jans.configapi.plugin.saml.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.saml.configuration.SamlConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.config.IdpConfig;
import io.jans.configapi.plugin.saml.model.config.SamlConf;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class KcIdpConfigService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    SamlConfigurationFactory samlConfigurationFactory;

    public SamlConf findSamlConf() {
        final String dn = samlConfigurationFactory.getKcIdpConfigurationDn();
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("KC IDP Configuration DN is undefined!");
        }

        logger.info(" dn:{}", dn);
        SamlConf samlConf = persistenceManager.find(dn, SamlConf.class, null);
        logger.info(" samlConf:{}", samlConf);

        return samlConf;
    }

    public void mergeSamlConfig(SamlConf samlConf) {
        samlConf.setRevision(samlConf.getRevision() + 1);
        persistenceManager.merge(samlConf);
    }

    public SamlAppConfiguration find() {
        return getSamlConf().getDynamicConf();
    }

    public String getSelectedIdp() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String selectedIdp = null;
        if (samlAppConfiguration != null) {
            selectedIdp = samlAppConfiguration.getSelectedIdp();
        }
        return selectedIdp;
    }

    public boolean isSamlEnabled() {
        final SamlConf samlConf = getSamlConf();
        logger.debug("samlConf.getDynamicConf():{}", samlConf.getDynamicConf());
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        boolean isSamlEnabled = false;
        if (samlAppConfiguration != null) {
            isSamlEnabled = samlAppConfiguration.isSamlEnabled();
        }
        return isSamlEnabled;
    }

    public String getIdpRootDir() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String idpRootDir = null;
        if (samlAppConfiguration != null) {
            idpRootDir = samlAppConfiguration.getIdpRootDir();
        }
        return idpRootDir;
    }

    public String getTrustRelationshipDn() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String trustRelationshipDn = null;
        if (samlAppConfiguration != null) {
            trustRelationshipDn = samlAppConfiguration.getSamlTrustRelationshipDn();
        }
        return trustRelationshipDn;
    }

    public String getSpMetadataFilePattern() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String spMetadataFilePattern = null;
        if (samlAppConfiguration != null) {
            spMetadataFilePattern = samlAppConfiguration.getSpMetadataFilePattern();
        }
        return spMetadataFilePattern;
    }

    public String getSpMetadataFile() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String spMetadataFile = null;
        if (samlAppConfiguration != null) {
            spMetadataFile = samlAppConfiguration.getSpMetadataFile();
        }
        return spMetadataFile;
    }

    public IdpConfig getSelectedIdpConfig() {
        final SamlConf samlConf = getSamlConf();

        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        IdpConfig selectedIdpConfig = null;
        if (samlAppConfiguration != null) {
            String selectedIdp = samlAppConfiguration.getSelectedIdp();
            List<IdpConfig> idpConfigs = samlAppConfiguration.getIdpConfigs();
            if (idpConfigs == null || idpConfigs.isEmpty()) {
                return selectedIdpConfig;
            }
            selectedIdpConfig = idpConfigs.stream()
                    .filter(e -> e.getConfigId() != null && e.getConfigId().equalsIgnoreCase(selectedIdp)).findAny().orElse(null);
        }
        return selectedIdpConfig;
    }

    public String getSelectedIdpConfigRootDir() {
        String rootDir = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();

        if (selectedIdpConfig == null) {
            return rootDir;
        }

        rootDir = selectedIdpConfig.getRootDir();
        return rootDir;
    }

    public String getSelectedIdpConfigMetadataTempDir() {
        String idpTempMetadataFolder = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();

        if (selectedIdpConfig == null) {
            return idpTempMetadataFolder;
        }

        idpTempMetadataFolder = selectedIdpConfig.getMetadataTempDir();
        return idpTempMetadataFolder;
    }

    public String getSelectedIdpConfigMetadataDir() {
        String metadataDir = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();

        if (selectedIdpConfig == null) {
            return metadataDir;
        }

        metadataDir = selectedIdpConfig.getMetadataDir();
        return metadataDir;
    }

    public String getSelectedIdpConfigID() {
        String configId = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();

        if (selectedIdpConfig == null) {
            return configId;
        }

        configId = selectedIdpConfig.getConfigId();
        return configId;
    }

    private SamlConf getSamlConf() {
        SamlConf samlConf = findSamlConf();
        if (samlConf == null) {
            throw new InvalidConfigurationException("SamlConf is undefined!");
        }
        logger.debug("  samlConf:{}, samlConf.getDynamicConf():{}", samlConf, samlConf.getDynamicConf());
        return samlConf;
    }
}
