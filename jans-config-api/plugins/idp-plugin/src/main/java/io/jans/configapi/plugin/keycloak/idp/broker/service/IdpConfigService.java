package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.configuration.IdpConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpAppConfiguration;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpConf;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class IdpConfigService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    IdpConfigurationFactory idpConfigurationFactory;

    public IdpConf findIdpConf() {
        final String dn = idpConfigurationFactory.getIdpConfigurationDn();
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("IDP Configuration DN is undefined!");
        }

        logger.info(" dn:{}", dn);
        IdpConf idpConf = persistenceManager.find(dn, IdpConf.class, null);
        logger.info(" idpConf:{}", idpConf);

        return idpConf;
    }

    public IdpConf getIdpConf() {
        final IdpConf idpConf = findIdpConf();
        if (idpConf == null) {
            throw new InvalidConfigurationException("IdpConf is undefined!");
        }
        logger.debug("  idpConf:{}, IdpConf.getDynamicConf():{}", idpConf, idpConf.getDynamicConf());
        return idpConf;
    }

    public void mergeIdpConfig(IdpConf idpConf) {
        idpConf.setRevision(idpConf.getRevision() + 1);
        persistenceManager.merge(idpConf);
    }

    public IdpAppConfiguration getIdpAppConfiguration() {
        return getIdpConf().getDynamicConf();
    }

    public boolean isIdpEnabled() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        boolean idpEnabled = false;
        if (idpAppConfiguration != null) {
            idpEnabled = idpAppConfiguration.isEnabled();
        }
        return idpEnabled;
    }

    public String getTrustedIdpDn() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String trustRelationshipDn = null;
        if (idpAppConfiguration != null) {
            trustRelationshipDn = idpAppConfiguration.getTrustedIdpDn();
        }
        return trustRelationshipDn;
    }

    public String getIdpRootDir() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String idpRootDir = null;
        if (idpAppConfiguration != null) {
            idpRootDir = idpAppConfiguration.getIdpRootDir();
        }
        return idpRootDir;
    }

    public String getIdpMetadataRootDir() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String idpMetadataRootDir = null;
        if (idpAppConfiguration != null) {
            idpMetadataRootDir = idpAppConfiguration.getIdpMetadataRootDir();
        }
        return idpMetadataRootDir;
    }
    
    public String getIdpMetadataTempDir() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String idpMetadataTempDir = null;
        if (idpAppConfiguration != null) {
            idpMetadataTempDir = idpAppConfiguration.getIdpMetadataTempDir();
        }
        return idpMetadataTempDir;
    }
    
    public String getIdpMetadataFilePattern() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String idpMetadataFilePattern = null;
        if (idpAppConfiguration != null) {
            idpMetadataFilePattern = idpAppConfiguration.getIdpMetadataFilePattern();
        }
        return idpMetadataFilePattern;
    }
    
    public String getIdpMetadataFile() {
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String idpMetadataFile = null;
        if (idpAppConfiguration != null) {
            idpMetadataFile = idpAppConfiguration.getIdpMetadataFile();
        }
        return idpMetadataFile;
    }
    
    public String getIdpMetadataTempDirFilePath(String idpMetaDataFN) {
        logger.debug("idpMetaDataFN:{}, getIdpMetadataTempDirFilePath():{}", idpMetaDataFN, getIdpMetadataTempDirFilePath());
        if (StringUtils.isBlank(getIdpMetadataTempDirFilePath())) {
            throw new InvalidConfigurationException("Failed to return IDP metadata file path as undefined!");
        }

        return getIdpMetadataTempDirFilePath() + idpMetaDataFN;
    }
    
    public String getIdpMetadataTempDirFilePath() {
        return getIdpMetadataTempDir() + File.separator;
    }
    
    public String getIdpMetadataFileName(String inum) {
        String id = StringHelper.removePunctuation(inum);
        return String.format(getIdpMetadataFilePattern(), id);
    }

    public String getSpMetadataUrl(String realm, String name) {
        logger.error("Get SP Metadata Url - realm:{}, name:{}", realm, name);
        IdpAppConfiguration idpAppConfiguration = getIdpAppConfiguration();
        String spMetadataUrl = null;
        if (idpAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(idpAppConfiguration.getServerUrl()).append(idpAppConfiguration.getSpMetadataUrl());
            spMetadataUrl = String.format(sb.toString(), realm, name);         
        }
        logger.error("SP Metadata Url - spMetadataUrl:{}", spMetadataUrl);
        return spMetadataUrl;
    }

}
