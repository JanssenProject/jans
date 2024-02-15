/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.server.service;

import org.slf4j.Logger;

import io.jans.config.GluuConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.EncryptionService;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 * @author Yuriy Movchan Date: 12/12/2023
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

    public void decryptSmtpPasswords(SmtpConfiguration smtpConfiguration) {
        if (smtpConfiguration == null) {
            return;
        }
        String password = smtpConfiguration.getSmtpAuthenticationAccountPassword();
        if (StringHelper.isNotEmpty(password)) {
            try {
                smtpConfiguration.setSmtpAuthenticationAccountPasswordDecrypted(encryptionService.decrypt(password));
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt SMTP user password", ex);
            }
        }
        password = smtpConfiguration.getKeyStorePassword();
        if (StringHelper.isNotEmpty(password)) {
            try {
                smtpConfiguration.setKeyStorePasswordDecrypted(encryptionService.decrypt(password));
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt Kestore password", ex);
            }
        }
    }

}

