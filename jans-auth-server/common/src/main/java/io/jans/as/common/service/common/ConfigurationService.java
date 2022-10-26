/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * GluuConfiguration service
 *
 * @author Reda Zerrad Date: 08.10.2012
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
public class ConfigurationService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private EncryptionService encryptionService;

    /**
     * Add new configuration
     *
     * @param configuration Configuration
     */
    public void addConfiguration(GluuConfiguration configuration) {
        persistenceEntryManager.persist(configuration);
    }

    /**
     * Update configuration entry
     *
     * @param configuration GluuConfiguration
     */
    public void updateConfiguration(GluuConfiguration configuration) {
        persistenceEntryManager.merge(configuration);
    }

    /**
     * Get configuration by inum
     *
     * @param inum Configuration Inum
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfigurationByInum(String inum) {
        return persistenceEntryManager.find(GluuConfiguration.class, getDnForConfiguration(inum));
    }

    /**
     * Get configuration
     *
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfiguration() {
        String configurationDn = staticConfiguration.getBaseDn().getConfiguration();
        if (StringHelper.isEmpty(configurationDn)) {
            return null;
        }

        return persistenceEntryManager.find(GluuConfiguration.class, configurationDn);
    }

    /**
     * Build DN string for configuration
     *
     * @param inum Inum
     * @return DN string for specified configuration or DN for configurations branch if inum is null
     * @throws Exception
     */
    public String getDnForConfiguration(String inum) {
        String baseDn = staticConfiguration.getBaseDn().getConfiguration();
        if (StringHelper.isEmpty(inum)) {
            return baseDn;
        }

        return String.format("inum=%s,%s", inum, baseDn);
    }

    public void decryptSmtpPassword(SmtpConfiguration smtpConfiguration) {
        if (smtpConfiguration == null) {
            return;
        }

        String password = smtpConfiguration.getPassword();
        if (StringHelper.isNotEmpty(password)) {
            try {
                smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(password));
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt SMTP user password", ex);
            }
        }
    }

}

