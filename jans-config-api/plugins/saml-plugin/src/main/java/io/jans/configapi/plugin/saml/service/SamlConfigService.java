package io.jans.configapi.plugin.saml.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.saml.configuration.SamlConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.config.IdpConfig;
import io.jans.configapi.plugin.saml.model.config.SamlConf;
import io.jans.orm.PersistenceEntryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import org.slf4j.Logger;

@ApplicationScoped
public class SamlConfigService {
    
    @Inject
    Logger logger;
    
    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    SamlConfigurationFactory samlConfigurationFactory;

    public SamlConf findSamlConf() {
        final String dn = samlConfigurationFactory.getSamlConfigurationDn();
        logger.error(" dn:{}",dn);
        SamlConf samlConf =  persistenceManager.find(dn, SamlConf.class, null);
        logger.error(" samlConf:{}",samlConf);
        
        return persistenceManager.find(dn, SamlConf.class, null);
    }

    public void mergeSamlConfig(SamlConf samlConf) {
        samlConf.setRevision(samlConf.getRevision() + 1);
        persistenceManager.merge(samlConf);
    }

    public SamlAppConfiguration find() {
        final SamlConf samlConf = findSamlConf();
        logger.error("  samlConf:{}, samlConf.getDynamicConf():{}", samlConf, samlConf.getDynamicConf());
        return samlConf.getDynamicConf();
    }

    public String getSelectedIdp() {
        final SamlConf samlConf = findSamlConf();
        logger.error("  samlConf:{}, samlConf.getDynamicConf():{}", samlConf, samlConf.getDynamicConf());
        SamlAppConfiguration samlAppConfiguration =  samlConf.getDynamicConf();
        String selectedIdp = null;
        if(samlAppConfiguration!=null) {
            selectedIdp = samlAppConfiguration.getSelectedIdp();
        }
        return selectedIdp;
    }
    
    public String getTrustRelationshipDn() {
        final SamlConf samlConf = findSamlConf();
        logger.error("  samlConf:{}, samlConf.getDynamicConf():{}", samlConf, samlConf.getDynamicConf());
        SamlAppConfiguration samlAppConfiguration =  samlConf.getDynamicConf();
        String trustRelationshipDn = null;
        if(samlAppConfiguration!=null) {
            trustRelationshipDn = samlAppConfiguration.getSamlTrustRelationshipDn();
        }
        return trustRelationshipDn;
    }
    
    public String getSpMetadataFilePattern() {
        final SamlConf samlConf = findSamlConf();
        logger.error("  samlConf:{}, samlConf.getDynamicConf():{}", samlConf, samlConf.getDynamicConf());
        SamlAppConfiguration samlAppConfiguration =  samlConf.getDynamicConf();
        String spMetadataFilePattern = null;
        if(samlAppConfiguration!=null) {
            spMetadataFilePattern = samlAppConfiguration.getSpMetadataFilePattern();
        }
        return spMetadataFilePattern;
    }
    
    public IdpConfig getSelectedIdpConfig() {
        final SamlConf samlConf = findSamlConf();
        logger.error("  samlConf:{}, samlConf.getDynamicConf():{}", samlConf, samlConf.getDynamicConf());
        SamlAppConfiguration samlAppConfiguration =  samlConf.getDynamicConf();
        IdpConfig selectedIdpConfig = null;
        if(samlAppConfiguration!=null) {
            String selectedIdp = samlAppConfiguration.getSelectedIdp();
            List<IdpConfig> idpConfigs = samlAppConfiguration.getIdpConfigs();
            if(idpConfigs == null || idpConfigs.isEmpty()) {
                return selectedIdpConfig;
            }
            selectedIdpConfig = idpConfigs.stream().
                    filter(e -> e.getConfigId()!=null && e.getConfigId().equals(selectedIdp)).
                    findAny().orElse(null);
        }
        return selectedIdpConfig;
    }
    
    public String getSelectedIdpConfigRootDir() {
        String rootDir = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();
        
        if(selectedIdpConfig==null) {
            return rootDir;
        }
        
        rootDir = selectedIdpConfig.getRootDir();
        return rootDir;
    }


    public String getSelectedIdpConfigMetadataTempDir() {
        String idpTempMetadataFolder = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();
        
        if(selectedIdpConfig==null) {
            return idpTempMetadataFolder;
        }
        
        idpTempMetadataFolder = selectedIdpConfig.getMetadataTempDir();
        return idpTempMetadataFolder;
    }
    
    public String getSelectedIdpConfigMetadataDir() {
        String metadataDir = null;
        IdpConfig selectedIdpConfig = getSelectedIdpConfig();
        
        if(selectedIdpConfig==null) {
            return metadataDir;
        }
        
        metadataDir = selectedIdpConfig.getMetadataDir();
        return metadataDir;
    }
}
