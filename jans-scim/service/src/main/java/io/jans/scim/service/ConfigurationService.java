/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import io.jans.model.*;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;

import io.jans.scim.model.JansConfiguration;
import io.jans.service.EncryptionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * JansConfiguration service
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

	private static final SimpleDateFormat PERIOD_DATE_FORMAT = new SimpleDateFormat("yyyyMM");
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public boolean contains(String configurationDn) {
		return persistenceEntryManager.contains(configurationDn, JansConfiguration.class);
	}

	/**
	 * Add new configuration
	 * 
	 * @param configuration
	 *            Configuration
	 */
	public void addConfiguration(JansConfiguration configuration) {
		persistenceEntryManager.persist(configuration);
	}

	/**
	 * Update configuration entry
	 * 
	 * @param configuration
	 *            JansConfiguration
	 */
	public void updateConfiguration(JansConfiguration configuration) {
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
		return persistenceEntryManager.contains(dn, JansConfiguration.class);
	}

	/**
	 * Get configuration by inum
	 * 
	 * @param inum
	 *            Configuration Inum
	 * @return Configuration
	 * @throws Exception
	 */
	public JansConfiguration getConfigurationByInum(String inum) {
		return persistenceEntryManager.find(JansConfiguration.class, getDnForConfiguration());
	}

	/**
	 * Get configuration
	 * 
	 * @return Configuration
	 * @throws Exception
	 */
	public JansConfiguration getConfiguration(String[] returnAttributes) {
		JansConfiguration result = null;
		result = persistenceEntryManager.find(getDnForConfiguration(), JansConfiguration.class, returnAttributes);
		return result;
	}

	/**
	 * Get configuration
	 * 
	 * @return Configuration
	 * @throws Exception
	 */
	public JansConfiguration getConfiguration() {
		return getConfiguration(null);
	}

	/**
	 * Get all configurations
	 * 
	 * @return List of attributes
	 * @throws Exception
	 */
	public List<JansConfiguration> getConfigurations() {
		return persistenceEntryManager.findEntries(getDnForConfiguration(), JansConfiguration.class, null);
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
		return new ScriptLocationType[] { ScriptLocationType.DB, ScriptLocationType.FILE };
	}

	public CustomScriptType[] getCustomScriptTypes() {
		return new CustomScriptType[] { CustomScriptType.PERSON_AUTHENTICATION, CustomScriptType.CONSENT_GATHERING,
				CustomScriptType.CLIENT_REGISTRATION,
				CustomScriptType.DYNAMIC_SCOPE, CustomScriptType.ID_GENERATOR, CustomScriptType.CACHE_REFRESH,
				CustomScriptType.UMA_RPT_POLICY, CustomScriptType.UMA_CLAIMS_GATHERING, CustomScriptType.UMA_RPT_CLAIMS, CustomScriptType.INTROSPECTION,
				CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS, CustomScriptType.APPLICATION_SESSION,
				CustomScriptType.END_SESSION, CustomScriptType.SCIM, CustomScriptType.POST_AUTHN,
				CustomScriptType.PERSISTENCE_EXTENSION, CustomScriptType.IDP };
	}

	public CustomScriptType[] getOthersCustomScriptTypes() {
		return new CustomScriptType[] { CustomScriptType.CONSENT_GATHERING, CustomScriptType.CLIENT_REGISTRATION,
				CustomScriptType.DYNAMIC_SCOPE, CustomScriptType.ID_GENERATOR, CustomScriptType.CACHE_REFRESH,
				CustomScriptType.UMA_RPT_POLICY, CustomScriptType.UMA_CLAIMS_GATHERING, CustomScriptType.UMA_RPT_CLAIMS, CustomScriptType.INTROSPECTION,
				CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS, CustomScriptType.APPLICATION_SESSION,
				CustomScriptType.END_SESSION, CustomScriptType.SCIM, CustomScriptType.POST_AUTHN,
				CustomScriptType.PERSISTENCE_EXTENSION, CustomScriptType.IDP, CustomScriptType.CIBA_END_USER_NOTIFICATION};
	}

	public void encryptedSmtpPasswords(SmtpConfiguration smtpConfiguration) {
		if (smtpConfiguration == null) {
			return;
		}
		String password = smtpConfiguration.getSmtpAuthenticationAccountPasswordDecrypted();
		if (StringHelper.isNotEmpty(password)) {
			try {
				String encryptedPassword = encryptionService.encrypt(password);
				smtpConfiguration.setSmtpAuthenticationAccountPassword(encryptedPassword);
			} catch (EncryptionException ex) {
				log.error("Failed to encrypt SMTP password", ex);
			}
		}
		password = smtpConfiguration.getKeyStorePasswordDecrypted();
		if (StringHelper.isNotEmpty(password)) {
			try {
				String encryptedPassword = encryptionService.encrypt(password);
				smtpConfiguration.setKeyStorePassword(encryptedPassword);
			} catch (EncryptionException ex) {
				log.error("Failed to encrypt Kestore password", ex);
			}
		}
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
				log.error("Failed to decrypt SMTP password", ex);
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

	public String getVersion() {
		String version = getClass().getPackage().getImplementationVersion();
		if (version == null) {
			Properties prop = new Properties();
			try (InputStream is = FacesContext.getCurrentInstance().getExternalContext()
					.getResourceAsStream("/META-INF/MANIFEST.MF")) {
				prop.load(is);
				version = prop.getProperty("Implementation-Version");
			} catch (IOException e) {
				log.error(e.toString());
			}
		}
		log.info("Starting App version " + version);
		if (version != null) {
			version = version.replace("-nightly", "");
			return version;
		}
		return "";
	}

}
