package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.configuration.IdpConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpAppConfiguration;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpConf;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
        IdpConf IdpConf = persistenceManager.find(dn, IdpConf.class, null);
        logger.info(" IdpConf:{}", IdpConf);

        return IdpConf;
    }

    public IdpConf getIdpConf() {
        final IdpConf idpConf = findIdpConf();
        if (idpConf == null) {
            throw new InvalidConfigurationException("IdpConf is undefined!");
        }
        logger.debug("  idpConf:{}, IdpConf.getDynamicConf():{}", idpConf, idpConf.getDynamicConf());
        return idpConf;
    }

    public void mergeIdpConfig(IdpConf IdpConf) {
        IdpConf.setRevision(IdpConf.getRevision() + 1);
        persistenceManager.merge(IdpConf);
    }

    public IdpAppConfiguration find() {
        return getIdpConf().getDynamicConf();
    }

    public boolean isIdpEnabled() {
        IdpAppConfiguration idpAppConfiguration = find();
        logger.debug("idpAppConfiguration:{}", idpAppConfiguration);
        boolean idpEnabled = false;
        if (idpAppConfiguration != null) {
            idpEnabled = idpAppConfiguration.isIdpEnabled();
        }
        return idpEnabled;
    }

    public String getTrustedIdpDn() {
        IdpAppConfiguration idpAppConfiguration = find();
        logger.debug("idpAppConfiguration:{}", idpAppConfiguration);
        String trustRelationshipDn = null;
        if (idpAppConfiguration != null) {
            trustRelationshipDn = idpAppConfiguration.getTrustedIdpDn();
        }
        return trustRelationshipDn;
    }

    public String getIdpRootDir() {
        IdpAppConfiguration idpAppConfiguration = find();
        logger.debug("idpAppConfiguration:{}", idpAppConfiguration);
        String idpRootDir = null;
        if (idpAppConfiguration != null) {
            idpRootDir = idpAppConfiguration.getIdpRootDir();
        }
        return idpRootDir;
    }

    public String getIdpTempDir() {
        IdpAppConfiguration idpAppConfiguration = find();
        logger.debug("idpAppConfiguration:{}", idpAppConfiguration);
        String idpTempDir = null;
        if (idpAppConfiguration != null) {
            idpTempDir = idpAppConfiguration.getTrustedIdpDn();
        }
        return idpTempDir;
    }

    public String getSpMetadataFilePattern() {
        IdpAppConfiguration idpAppConfiguration = find();
        logger.debug("idpAppConfiguration:{}", idpAppConfiguration);
        String spMetadataFilePattern = null;
        if (idpAppConfiguration != null) {
            spMetadataFilePattern = idpAppConfiguration.getSpMetadataFilePattern();
        }
        return spMetadataFilePattern;
    }

    public String getSpMetadataFile() {
        IdpAppConfiguration idpAppConfiguration = find();
        logger.debug("idpAppConfiguration:{}", idpAppConfiguration);
        String spMetadataFile = null;
        if (idpAppConfiguration != null) {
            spMetadataFile = idpAppConfiguration.getSpMetadataFile();
        }
        return spMetadataFile;
    }

}
