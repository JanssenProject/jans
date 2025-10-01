/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.keycloak.link.server.service;

import io.jans.config.GluuConfiguration;
import io.jans.keycloak.link.model.config.StaticConfiguration;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.EncryptionService;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

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

