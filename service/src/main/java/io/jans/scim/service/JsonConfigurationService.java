/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import java.io.IOException;
import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import io.jans.config.oxtrust.AppConfiguration;
import io.jans.config.oxtrust.AttributeResolverConfiguration;
import io.jans.config.oxtrust.CacheRefreshConfiguration;
import io.jans.config.oxtrust.DbApplicationConfiguration;
import io.jans.config.oxtrust.ImportPersonConfig;
import io.jans.config.oxtrust.LdapOxAuthConfiguration;
import io.jans.config.oxtrust.LdapOxTrustConfiguration;
import io.jans.scim.service.config.ConfigurationFactory;
import io.jans.scim.model.GluuConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.service.JsonService;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.cache.RedisConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.util.security.StringEncrypter;
import org.slf4j.Logger;

/**
 * Provides operations with JSON oxAuth/oxTrust configuration
 * 
 * @author Yuriy Movchan Date: 12.15.2010
 */
@ApplicationScoped
@Named("jsonConfigurationService")
public class JsonConfigurationService implements Serializable {

	private static final long serialVersionUID = -3840968275007784641L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;
	@Inject
	private JsonService jsonService;

	@Inject
	private StringEncrypter stringEncrypter;

	@Inject
	private ConfigurationFactory<?> configurationFactory;

	@Inject
	private ConfigurationService configurationService;

	public AppConfiguration getOxTrustappConfiguration() {
		LdapOxTrustConfiguration ldapOxTrustConfiguration = getOxTrustConfiguration();
		return ldapOxTrustConfiguration.getApplication();
	}

	public CacheConfiguration getOxMemCacheConfiguration() {
		return configurationService.getConfiguration().getCacheConfiguration();
	}

	public DocumentStoreConfiguration getDocumentStoreConfiguration() {
		return configurationService.getConfiguration().getDocumentStoreConfiguration();
	}

	public ImportPersonConfig getOxTrustImportPersonConfiguration() {
		return getOxTrustConfiguration().getImportPersonConfig();
	}

	public CacheRefreshConfiguration getOxTrustCacheRefreshConfiguration() {
		return getOxTrustConfiguration().getCacheRefresh();
	}

	private LdapOxTrustConfiguration getOxTrustConfiguration() {
		String configurationDn = configurationFactory.getConfigurationDn();
		return loadOxTrustConfig(configurationDn);
	}

	public String getOxAuthDynamicConfigJson() throws IOException {
		String configurationDn = configurationFactory.getConfigurationDn();
		return loadOxAuthConfig(configurationDn).getDynamicConf();
	}

	public io.jans.as.model.configuration.AppConfiguration getOxauthAppConfiguration() throws IOException {
		return jsonService.jsonToObject(getOxAuthDynamicConfigJson(),
				io.jans.as.model.configuration.AppConfiguration.class);
	}

	public boolean saveOxTrustappConfiguration(AppConfiguration oxTrustappConfiguration) {
		LdapOxTrustConfiguration ldapOxTrustConfiguration = getOxTrustConfiguration();
		ldapOxTrustConfiguration.setApplication(oxTrustappConfiguration);
		ldapOxTrustConfiguration.setRevision(ldapOxTrustConfiguration.getRevision() + 1);
		persistenceEntryManager.merge(ldapOxTrustConfiguration);
		return true;
	}

	public boolean saveOxTrustImportPersonConfiguration(ImportPersonConfig oxTrustImportPersonConfiguration) {
		LdapOxTrustConfiguration ldapOxTrustConfiguration = getOxTrustConfiguration();
		ldapOxTrustConfiguration.setImportPersonConfig(oxTrustImportPersonConfiguration);
		ldapOxTrustConfiguration.setRevision(ldapOxTrustConfiguration.getRevision() + 1);
		persistenceEntryManager.merge(ldapOxTrustConfiguration);
		return true;
	}

	public boolean saveOxTrustCacheRefreshConfiguration(CacheRefreshConfiguration oxTrustCacheRefreshConfiguration) {
		LdapOxTrustConfiguration ldapOxTrustConfiguration = getOxTrustConfiguration();
		ldapOxTrustConfiguration.setCacheRefresh(oxTrustCacheRefreshConfiguration);
		ldapOxTrustConfiguration.setRevision(ldapOxTrustConfiguration.getRevision() + 1);
		persistenceEntryManager.merge(ldapOxTrustConfiguration);
		return true;
	}

	public boolean saveOxTrustAttributeResolverConfigurationConfiguration(
			AttributeResolverConfiguration attributeResolverConfiguration) {
		LdapOxTrustConfiguration ldapOxTrustConfiguration = getOxTrustConfiguration();
		ldapOxTrustConfiguration.setAttributeResolverConfig(attributeResolverConfiguration);
		ldapOxTrustConfiguration.setRevision(ldapOxTrustConfiguration.getRevision() + 1);
		persistenceEntryManager.merge(ldapOxTrustConfiguration);
		return true;
	}

	public boolean saveOxAuthAppConfiguration(io.jans.as.model.configuration.AppConfiguration appConfiguration) {
		try {
			String appConfigurationJson = jsonService.objectToJson(appConfiguration);
			return saveOxAuthDynamicConfigJson(appConfigurationJson);
		} catch (IOException e) {
			log.error("Failed to serialize AppConfiguration", e);
		}
		return false;
	}

	public boolean saveOxAuthDynamicConfigJson(String oxAuthDynamicConfigJson) throws IOException {
		String configurationDn = configurationFactory.getConfigurationDn();

		LdapOxAuthConfiguration ldapOxAuthConfiguration = loadOxAuthConfig(configurationDn);
		ldapOxAuthConfiguration.setDynamicConf(oxAuthDynamicConfigJson);
		ldapOxAuthConfiguration.setRevision(ldapOxAuthConfiguration.getRevision() + 1);
		persistenceEntryManager.merge(ldapOxAuthConfiguration);
		return true;
	}

	public boolean saveOxMemCacheConfiguration(CacheConfiguration cachedConfiguration) {
		encrypPassword(cachedConfiguration.getRedisConfiguration());
		GluuConfiguration gluuConfiguration = configurationService.getConfiguration();
		gluuConfiguration.setCacheConfiguration(cachedConfiguration);
		configurationService.updateConfiguration(gluuConfiguration);
		return true;
	}

	public boolean saveDocumentStoreConfiguration(DocumentStoreConfiguration documentStoreConfiguration) {
		GluuConfiguration gluuConfiguration = configurationService.getConfiguration();
		gluuConfiguration.setDocumentStoreConfiguration(documentStoreConfiguration);
		configurationService.updateConfiguration(gluuConfiguration);
		return true;
	}

	private LdapOxTrustConfiguration loadOxTrustConfig(String configurationDn) {
		try {
			LdapOxTrustConfiguration conf = persistenceEntryManager.find(LdapOxTrustConfiguration.class,
					configurationDn);

			return conf;
		} catch (BasePersistenceException ex) {
			log.error("Failed to load configuration from LDAP");
		}

		return null;
	}

	private LdapOxAuthConfiguration loadOxAuthConfig(String configurationDn) {
		try {
			configurationDn = configurationDn.replace("ou=oxtrust", "ou=oxauth");
			LdapOxAuthConfiguration conf = persistenceEntryManager.find(LdapOxAuthConfiguration.class, configurationDn);
			return conf;
		} catch (BasePersistenceException ex) {
			log.error("Failed to load configuration from LDAP");
		}

		return null;
	}
	
	private void encrypPassword(RedisConfiguration redisConfiguration) {
        try {
            String password = redisConfiguration.getPassword();
            if (StringUtils.isNotBlank(password)) {
                redisConfiguration.setPassword(stringEncrypter.encrypt(password));
                log.trace("Decrypted redis password successfully.");
            }
        } catch (StringEncrypter.EncryptionException e) {
            log.error("Error during redis password decryption", e);
        }
    }

	public DbApplicationConfiguration loadFido2Configuration() {
		try {
			String configurationDn = configurationFactory.getBaseConfiguration()
					.getString("fido2_ConfigurationEntryDN");
			DbApplicationConfiguration conf = persistenceEntryManager.find(DbApplicationConfiguration.class,
					configurationDn);
			return conf;
		} catch (BasePersistenceException ex) {
			log.error("Failed to load Fido2 configuration from LDAP");
		}

		return null;
	}

	public void saveFido2Configuration(String fido2ConfigJson) {
		DbApplicationConfiguration fido2Configuration = loadFido2Configuration();
		fido2Configuration.setDynamicConf(fido2ConfigJson);
		fido2Configuration.setRevision(fido2Configuration.getRevision() + 1);
		persistenceEntryManager.merge(fido2Configuration);
	}

}
