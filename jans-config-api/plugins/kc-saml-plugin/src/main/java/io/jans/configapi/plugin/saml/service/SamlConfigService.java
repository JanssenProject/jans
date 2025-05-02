package io.jans.configapi.plugin.saml.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.saml.configuration.SamlConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.config.SamlConf;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

    // Config handling methods
    public SamlConf findSamlConf() {
        final String dn = samlConfigurationFactory.getSamlConfigurationDn();
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("Saml Configuration DN is undefined!");
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

    // Utility methods
    public String getTrustRelationshipDn() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String trustRelationshipDn = null;
        if (samlAppConfiguration != null) {
            trustRelationshipDn = samlAppConfiguration.getSamlTrustRelationshipDn();
        }
        return trustRelationshipDn;
    }

    public String getTrustedIdpDn() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String trustedIdpDn = null;
        if (samlAppConfiguration != null) {
            trustedIdpDn = samlAppConfiguration.getTrustedIdpDn();
        }
        return trustedIdpDn;
    }

    public boolean isSamlEnabled() {
        final SamlConf samlConf = getSamlConf();
        logger.debug("samlConf.getDynamicConf():{}", samlConf.getDynamicConf());
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        boolean isSamlEnabled = false;
        if (samlAppConfiguration != null) {
            isSamlEnabled = samlAppConfiguration.isEnabled();
        }
        return isSamlEnabled;
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

    public String getServerUrl() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String serverUrl = null;
        if (samlAppConfiguration != null) {
            serverUrl = samlAppConfiguration.getServerUrl();
        }
        return serverUrl;
    }

    public String getRealm() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String realm = null;
        if (samlAppConfiguration != null) {
            realm = samlAppConfiguration.getRealm();
        }
        return realm;
    }

    public String getClientId() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String clientId = null;
        if (samlAppConfiguration != null) {
            clientId = samlAppConfiguration.getClientId();
        }
        return clientId;
    }

    public String getClientSecret() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String clientSecret = null;
        if (samlAppConfiguration != null) {
            clientSecret = samlAppConfiguration.getClientSecret();
        }
        return clientSecret;
    }

    public String getGrantType() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String grantType = null;
        if (samlAppConfiguration != null) {
            grantType = samlAppConfiguration.getGrantType();
        }
        return grantType;
    }

    public String getScope() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String scope = null;
        if (samlAppConfiguration != null) {
            scope = samlAppConfiguration.getScope();
        }
        return scope;
    }

    public String getUsername() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String username = null;
        if (samlAppConfiguration != null) {
            username = samlAppConfiguration.getUsername();
        }
        return username;
    }

    public String getPassword() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String password = null;
        if (samlAppConfiguration != null) {
            password = samlAppConfiguration.getPassword();
        }
        return password;
    }

    public String getSpMetadataUrl(String realm, String name) {
        logger.debug("Get SP Metadata Url - realm:{}, name:{}", realm, name);
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String spMetadataUrl = null;
        if (samlAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(samlAppConfiguration.getServerUrl()).append(samlAppConfiguration.getSpMetadataUrl());
            spMetadataUrl = String.format(sb.toString(), realm, name);
        }
        logger.debug("SP Metadata Url - spMetadataUrl:{}", spMetadataUrl);
        return spMetadataUrl;
    }

    public String getTokenUrl(String realm) {
        logger.debug("Get SAML Token Url - realm:{}", realm);
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String tokenUrl = null;
        if (samlAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(samlAppConfiguration.getServerUrl()).append(samlAppConfiguration.getTokenUrl());
            tokenUrl = String.format(sb.toString(), realm);
        }
        logger.debug("SAML Token Url - tokenUrl:{}", tokenUrl);
        return tokenUrl;
    }

    public String getIdpUrl(String realm) {
        logger.debug("Get SAML IDP Url - realm:{}", realm);
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String idpUrl = null;
        if (samlAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(samlAppConfiguration.getServerUrl()).append(samlAppConfiguration.getIdpUrl());
            idpUrl = String.format(sb.toString(), realm);
        }
        logger.debug("SAML IDP Url - idpUrl:{}", idpUrl);
        return idpUrl;
    }
    
    public String getExtIDPTokenUrl(String realm, String idpAliasName ) {
        logger.debug("Get SAML External IDP Url - realm:{}, idpAliasName:{}", realm, idpAliasName);
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String extIDPTokenUrl = null;
        if (samlAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(samlAppConfiguration.getServerUrl()).append(samlAppConfiguration.getExtIDPTokenUrl());
            extIDPTokenUrl = String.format(sb.toString(), realm, idpAliasName);
        }
        logger.debug("SAML Ext IDP Url - extIDPTokenUrl:{}", extIDPTokenUrl);
        return extIDPTokenUrl;
    }
    
    public String getExtIDPRedirectUrl(String realm, String clientId, String redirectUrl, String responseType, String idpAliasName ) {
        logger.debug("Get SAML External IDP Redirect Url - realm:{}, clientId:{}, redirectUrl:{}, responseType:{}, idpAliasName:{}", realm, clientId, redirectUrl, responseType, idpAliasName);
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String extIDPRedirectUrl = null;
        if (samlAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(samlAppConfiguration.getServerUrl()).append(samlAppConfiguration.getExtIDPRedirectUrl());
            extIDPRedirectUrl = String.format(sb.toString(), realm, clientId, redirectUrl,  responseType,  idpAliasName);
        }
        logger.debug("SAML External IDP Redirect  - extIDPRedirectUrl:{}", extIDPRedirectUrl);
        return extIDPRedirectUrl;
    }

    public String getIdpMetadataImportUrl(String realm) {
        logger.debug("Get SAML IDP Metadata Import Url - realm:{}", realm);
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String idpMetadataImportUrl = null;
        if (samlAppConfiguration != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(samlAppConfiguration.getServerUrl()).append(samlAppConfiguration.getIdpMetadataImportUrl());
            idpMetadataImportUrl = String.format(sb.toString(), realm);
        }
        logger.debug("SAML IDP Metadata Import Url - idpMetadataImportUrl:{}", idpMetadataImportUrl);
        return idpMetadataImportUrl;
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

    public String getIdpMetadataDir() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String idpMetadataDir = null;
        if (samlAppConfiguration != null) {
            idpMetadataDir = samlAppConfiguration.getIdpMetadataDir();
        }
        return idpMetadataDir;
    }

    public String getIdpMetadataTempDir() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String idpMetadataTempDir = null;
        if (samlAppConfiguration != null) {
            idpMetadataTempDir = samlAppConfiguration.getIdpMetadataTempDir();
        }
        return idpMetadataTempDir;
    }

    public String getIdpMetadataFilePattern() {
        return Constants.IDP_METADATA_FILE_PATTERN;
    }

    public String getIdpMetadataFile() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String idpMetadataFile = null;
        if (samlAppConfiguration != null) {
            idpMetadataFile = samlAppConfiguration.getIdpMetadataFile();
        }
        return idpMetadataFile;
    }

    public String getSpMetadataDir() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String spMetadataDir = null;
        if (samlAppConfiguration != null) {
            spMetadataDir = samlAppConfiguration.getSpMetadataDir();
        }
        return spMetadataDir;
    }

    public String getSpMetadataTempDir() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        String spMetadataTempDir = null;
        if (samlAppConfiguration != null) {
            spMetadataTempDir = samlAppConfiguration.getSpMetadataTempDir();
        }
        return spMetadataTempDir;
    }

    public String getSpMetadataFilePattern() {
        return Constants.SP_METADATA_FILE_PATTERN;
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

    public boolean isIgnoreValidation() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        boolean ignoreValidation = false;
        if (samlAppConfiguration != null) {
            ignoreValidation = samlAppConfiguration.isIgnoreValidation();
        }
        return ignoreValidation;
    }
    
    
    public boolean isSetConfigDefaultValue() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        boolean setConfigDefaultValue = false;
        if (samlAppConfiguration != null) {
            setConfigDefaultValue = samlAppConfiguration.isSetConfigDefaultValue();
        }
        return setConfigDefaultValue;
    }

    public List<String> getIdpMetadataMandatoryAttributes() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        List<String> idpMetadataMandatoryAttributes = null;
        if (samlAppConfiguration != null) {
            idpMetadataMandatoryAttributes = samlAppConfiguration.getIdpMetadataMandatoryAttributes();
        }
        return idpMetadataMandatoryAttributes;
    }

    public List<String> getKcAttributes() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        List<String> kcAttributes = null;
        if (samlAppConfiguration != null) {
            kcAttributes = samlAppConfiguration.getKcAttributes();
        }
        return kcAttributes;
    }

    public List<String> getKcSamlConfig() {
        final SamlConf samlConf = getSamlConf();
        SamlAppConfiguration samlAppConfiguration = samlConf.getDynamicConf();
        List<String> kcSamlConfig = null;
        if (samlAppConfiguration != null) {
            kcSamlConfig = samlAppConfiguration.getKcSamlConfig();
        }
        return kcSamlConfig;
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
