/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service.config;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;

//import javax.inject.Inject;
//import javax.inject.Named;

import io.jans.cacherefresh.model.GluuConfiguration;
import io.jans.cacherefresh.model.ProgrammingLanguage;
import io.jans.cacherefresh.service.EncryptionService;
import io.jans.cacherefresh.service.OrganizationService;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.ScriptLocationType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.MetricService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
        return getPersistenceEntryManager().contains(configurationDn, GluuConfiguration.class);
    }

    /**
     * Add new configuration
     * 
     * @param configuration
     *            Configuration
     */
    public void addConfiguration(GluuConfiguration configuration) {
        getPersistenceEntryManager().persist(configuration);
    }

    /**
     * Update configuration entry
     * 
     * @param configuration
     *            GluuConfiguration
     */
    public void updateConfiguration(GluuConfiguration configuration) {
        try {
            getPersistenceEntryManager().merge(configuration);
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
        return getPersistenceEntryManager().contains(dn, GluuConfiguration.class);
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
        return getPersistenceEntryManager().find(GluuConfiguration.class, getDnForConfiguration());
    }

    /**
     * Get configuration
     * 
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfiguration(String[] returnAttributes) {
        GluuConfiguration result = null;
        result = getPersistenceEntryManager().find(getDnForConfiguration(), GluuConfiguration.class, returnAttributes);
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
        return getPersistenceEntryManager().findEntries(getDnForConfiguration(), GluuConfiguration.class, null);
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

    public PersistenceEntryManager getPersistenceEntryManager() {
        return persistenceEntryManager;
    }

    public void setPersistenceEntryManager(PersistenceEntryManager persistenceEntryManager) {
        this.persistenceEntryManager = persistenceEntryManager;
    }
}
