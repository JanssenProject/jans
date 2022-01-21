/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.configuration;

import io.jans.exception.ConfigurationException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.util.properties.FileConfiguration;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigurationFactory {

    public static final String CONFIGURATION_ENTRY_DN = "scim_ConfigurationEntryDN";

    @Inject
    private Logger log;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;
    
    static {
        if (System.getProperty("jans.base") != null) {
            BASE_DIR = System.getProperty("jans.base");
        } else if ((System.getProperty("catalina.base") != null)
                && (System.getProperty("catalina.base.ignore") == null)) {
            BASE_DIR = System.getProperty("catalina.base");
        } else if (System.getProperty("catalina.home") != null) {
            BASE_DIR = System.getProperty("catalina.home");
        } else if (System.getProperty("jboss.home.dir") != null) {
            BASE_DIR = System.getProperty("jboss.home.dir");
        } else {
            BASE_DIR = null;
        }
    }

    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;
    private static final String BASE_PROPERTIES_FILE = DIR + "jans.properties";
    private static final String APP_PROPERTIES_FILE = DIR + "scim.properties";

    private FileConfiguration baseConfiguration;
    private PersistenceConfiguration persistenceConfiguration;


    @PostConstruct
    public void init() {

        try {
            log.trace("ScimConfigurationFactory::loadConfigurationFromLdap() - persistanceFactoryService:{} ",
                    persistanceFactoryService);
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);

            loadBaseConfiguration();

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public FileConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }

    @Produces
    @ApplicationScoped
    public PersistenceConfiguration getPersistenceConfiguration() {
        return persistenceConfiguration;
    }   
   
    public String getConfigurationDn() {
        return this.baseConfiguration.getString(CONFIGURATION_ENTRY_DN);
    }

  
    private void loadBaseConfiguration() {
        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);
    }

    private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
        try {
            return new FileConfiguration(fileName);
        } catch (Exception ex) {
            if (isMandatory) {
                log.error("Failed to load configuration from {}", fileName, ex);
                throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
            }
        }

        return null;
    }

}
