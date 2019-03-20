/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.persist.PersistenceEntryManager;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.slf4j.Logger;
import org.xdi.model.SmtpConfiguration;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter.EncryptionException;

/**
 * GluuConfiguration service
 *
 * @author Reda Zerrad Date: 08.10.2012
 */
@Stateless
@Named
public class ConfigurationService {

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private StaticConfiguration staticConfiguration;
    
    @Inject
    private EncryptionService encryptionService;

    /**
	 * Add new configuration
	 * @param configuration Configuration
	 */
	public void addConfiguration(GluuConfiguration configuration) {
		ldapEntryManager.persist(configuration);
	}

	/**
	 * Update configuration entry
	 * @param configuration GluuConfiguration
	 */
	public void updateConfiguration(GluuConfiguration configuration) {
		ldapEntryManager.merge(configuration);
	}

	/**
	 * Check if LDAP server contains configuration with specified attributes
	 * @return True if configuration with specified attributes exist
	 */
	public boolean containsConfiguration(GluuConfiguration configuration) {
		return ldapEntryManager.contains(configuration);
	}

	/**
	 * Get configuration by inum
	 * @param inum Configuration Inum
	 * @return Configuration
	 * @throws Exception 
	 */
	public GluuConfiguration getConfigurationByInum(String inum) {
		return ldapEntryManager.find(GluuConfiguration.class, getDnForConfiguration(inum));
	}

	/**
	 * Get configuration
	 * @return Configuration
	 * @throws Exception 
	 */
	public GluuConfiguration getConfiguration() {
		String configurationDn = staticConfiguration.getBaseDn().getConfiguration();
		if (StringHelper.isEmpty(configurationDn)) {
			return null;
		}

		return ldapEntryManager.find(GluuConfiguration.class, configurationDn);
	}

	/**
	 * Build DN string for configuration
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

