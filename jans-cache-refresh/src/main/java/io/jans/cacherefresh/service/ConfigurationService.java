/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import io.jans.cacherefresh.model.GluuConfiguration;
import io.jans.cacherefresh.model.ProgrammingLanguage;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.ScriptLocationType;
import io.jans.model.SmtpConfiguration;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.MetricService;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;

/**
 * GluuConfiguration service
 * 
 * @author Reda Zerrad Date: 08.10.2012
 */
@ApplicationScoped
@Named("configurationService")
public class ConfigurationService implements Serializable {

    private static final long serialVersionUID = 8842838732456296435L;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private EncryptionService encryptionService;
    
    @Inject
    private MetricService metricService;

    private static final SimpleDateFormat PERIOD_DATE_FORMAT = new SimpleDateFormat("yyyyMM");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public boolean contains(String configurationDn) {
        return persistenceEntryManager.contains(configurationDn, GluuConfiguration.class);
    }

    /**
     * Add new configuration
     * 
     * @param configuration
     *            Configuration
     */
    public void addConfiguration(GluuConfiguration configuration) {
        persistenceEntryManager.persist(configuration);
    }

    /**
     * Update configuration entry
     * 
     * @param configuration
     *            GluuConfiguration
     */
    public void updateConfiguration(GluuConfiguration configuration) {
        try {
            persistenceEntryManager.merge(configuration);
        } catch (Exception e) {
            log.info("", e);
        }
    }

    /**
     * Check if LDAP server contains configuration with specified attributes
     * 
     * @return True if configuration with specified attributes exist
     */
    public boolean containsConfiguration(String dn) {
        return persistenceEntryManager.contains(dn, GluuConfiguration.class);
    }

    /**
     * Get configuration by inum
     * 
     * @param inum
     *            Configuration Inum
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfigurationByInum(String inum) {
        return persistenceEntryManager.find(GluuConfiguration.class, getDnForConfiguration());
    }

    /**
     * Get configuration
     * 
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfiguration(String[] returnAttributes) {
        GluuConfiguration result = null;
        result = persistenceEntryManager.find(getDnForConfiguration(), GluuConfiguration.class, returnAttributes);
        return result;
    }


    /**
     * Get configuration
     * 
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfiguration() {
        return getConfiguration(null);
    }


    /**
     * Get all configurations
     * 
     * @return List of attributes
     * @throws Exception
     */
    public List<GluuConfiguration> getConfigurations() {
        return persistenceEntryManager.findEntries(getDnForConfiguration(), GluuConfiguration.class, null);
    }

    /**
     * Build DN string for configuration
     * 
     * @return DN string for specified configuration or DN for configurations branch
     *         if inum is null
     * @throws Exception
     */
    public String getDnForConfiguration() {
        String baseDn = organizationService.getBaseDn();
        return String.format("ou=configuration,%s", baseDn);
    }

    public AuthenticationScriptUsageType[] getScriptUsageTypes() {
        return new AuthenticationScriptUsageType[] { AuthenticationScriptUsageType.INTERACTIVE,
                AuthenticationScriptUsageType.SERVICE, AuthenticationScriptUsageType.BOTH };
    }

    public ProgrammingLanguage[] getProgrammingLanguages() {
        return new ProgrammingLanguage[] { ProgrammingLanguage.PYTHON };
    }

    public ScriptLocationType[] getLocationTypes() {
        return new ScriptLocationType[] { ScriptLocationType.LDAP, ScriptLocationType.FILE };
    }

    public CustomScriptType[] getCustomScriptTypes() {
        return new CustomScriptType[] { CustomScriptType.PERSON_AUTHENTICATION, CustomScriptType.CONSENT_GATHERING, CustomScriptType.CLIENT_REGISTRATION,
                CustomScriptType.DYNAMIC_SCOPE, CustomScriptType.ID_GENERATOR, CustomScriptType.CACHE_REFRESH,
                CustomScriptType.UMA_RPT_POLICY, CustomScriptType.UMA_CLAIMS_GATHERING, CustomScriptType.UMA_RPT_CLAIMS,
                CustomScriptType.INTROSPECTION, CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS,
                CustomScriptType.APPLICATION_SESSION, CustomScriptType.END_SESSION, CustomScriptType.SCIM,
                CustomScriptType.POST_AUTHN, CustomScriptType.PERSISTENCE_EXTENSION, CustomScriptType.IDP,
                CustomScriptType.REVOKE_TOKEN };
    }

    public CustomScriptType[] getOthersCustomScriptTypes() {
        return new CustomScriptType[] { CustomScriptType.CONSENT_GATHERING, CustomScriptType.CLIENT_REGISTRATION,
                CustomScriptType.DYNAMIC_SCOPE, CustomScriptType.ID_GENERATOR, CustomScriptType.CACHE_REFRESH,
                CustomScriptType.UMA_RPT_POLICY, CustomScriptType.UMA_CLAIMS_GATHERING, CustomScriptType.UMA_RPT_CLAIMS,
                CustomScriptType.INTROSPECTION, CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS,
                CustomScriptType.APPLICATION_SESSION, CustomScriptType.END_SESSION, CustomScriptType.SCIM,
                CustomScriptType.POST_AUTHN, CustomScriptType.PERSISTENCE_EXTENSION, CustomScriptType.IDP,
                CustomScriptType.REVOKE_TOKEN, CustomScriptType.UPDATE_TOKEN, CustomScriptType.CIBA_END_USER_NOTIFICATION };
    }

    public void encryptSmtpPassword(SmtpConfiguration smtpConfiguration) {
        if (smtpConfiguration == null) {
            return;
        }
        String password = smtpConfiguration.getPasswordDecrypted();
        if (StringHelper.isNotEmpty(password)) {
            try {
                String encryptedPassword = encryptionService.encrypt(password);
                smtpConfiguration.setPassword(encryptedPassword);
            } catch (StringEncrypter.EncryptionException ex) {
                log.error("Failed to encrypt SMTP password", ex);
            }
        }
    }

    public void decryptSmtpPassword(SmtpConfiguration smtpConfiguration) {
        if (smtpConfiguration == null) {
            return;
        }
        String password = smtpConfiguration.getPassword();
        if (StringHelper.isNotEmpty(password)) {
            try {
                smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(password));
            } catch (StringEncrypter.EncryptionException ex) {
                log.error("Failed to decrypt SMTP password", ex);
            }
        }
    }

}
