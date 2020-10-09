/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.scim.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.config.oxtrust.LdapOxAuthConfiguration;
import org.gluu.model.GluuStatus;
import org.gluu.model.ProgrammingLanguage;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.persist.model.base.GluuBoolean;
import org.gluu.service.BaseCacheService;
import org.gluu.service.CacheService;
import org.gluu.service.LocalCacheService;
import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import io.jans.scim.model.GluuOrganization;
import io.jans.scim.util.OxTrustConstants;

/**
 * Provides operations with organization
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@ApplicationScoped
@Priority(value = 5)
@Named("organizationService")
public class OrganizationService extends org.gluu.service.OrganizationService {

	private static final long serialVersionUID = -1959146007518514678L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private CacheService cacheService;

	@Inject
	private LocalCacheService localCacheService;

	@Inject
	private AppConfiguration appConfiguration;

	/**
	 * Update organization entry
	 * 
	 * @param organization
	 *            Organization
	 */
	public void updateOrganization(GluuOrganization organization) {
		persistenceEntryManager.merge(organization);

	}

	/**
	 * Check if LDAP server contains organization with specified attributes
	 * 
	 * @return True if organization with specified attributes exist
	 */
	public boolean containsOrganization(String dn) {
		return persistenceEntryManager.contains(dn, GluuOrganization.class);
	}

	/**
	 * Get organization
	 * 
	 * @return Organization
	 */
	public GluuOrganization getOrganization() {
		BaseCacheService usedCacheService = getCacheService();
		String key = getDnForOrganization();
		GluuOrganization organization = (GluuOrganization) usedCacheService.get(key);
		if (organization == null) {
			organization = persistenceEntryManager.find(GluuOrganization.class, key);
			usedCacheService.put(key, organization);
		}

		return organization;
	}

	public String getOrgName() {
		return getOrganization().getDisplayName();
	}

	public String getDnForOrganization() {
		return getDnForOrganization(appConfiguration.getBaseDN());
	}

	/**
	 * Returns custom message defined for the organization
	 * 
	 * @param customMessageId
	 *            message id
	 * @return custom message
	 */
	public String getOrganizationCustomMessage(String customMessageId) {
		BaseCacheService usedCacheService = getCacheService();

		GluuOrganization organization = getOrganization();

		String key = OxTrustConstants.CACHE_ORGANIZATION_CUSTOM_MESSAGE_KEY;
		@SuppressWarnings("unchecked")
		Map<String, String> organizationCustomMessage = (Map<String, String>) usedCacheService.get(key);
		if (organizationCustomMessage == null) {
			organizationCustomMessage = new HashMap<String, String>();

			String[] customMessages = organization.getCustomMessages();
			if (ArrayHelper.isNotEmpty(customMessages)) {
				for (String customMessage : customMessages) {
					int idx = customMessage.indexOf(':');
					if ((idx > 0) && (idx + 1 < customMessage.length())) {
						String msgKey = customMessage.substring(0, idx).trim();
						String msgValue = customMessage.substring(idx + 1).trim();

						if (StringHelper.isNotEmpty(msgKey) && StringHelper.isNotEmpty(msgValue)) {
							organizationCustomMessage.put(msgKey, msgValue);
						}
					}
				}
			}
			usedCacheService.put(key, organizationCustomMessage);
		}

		return organizationCustomMessage.get(customMessageId);
	}

	public String[] buildOrganizationCustomMessages(String[][] customMessages) {
		List<String> result = new ArrayList<String>();

		for (String[] customMessage : customMessages) {
			if (ArrayHelper.isEmpty(customMessage) || customMessage.length != 2) {
				continue;
			}
			String msgKey = customMessage[0];
			String msgValue = customMessage[1];

			if (StringHelper.isNotEmpty(msgKey) && StringHelper.isNotEmpty(msgValue)) {
				result.add(msgKey + ": " + msgValue);
			}
		}

		return result.toArray(new String[0]);
	}

	/**
	 * Build DN string for organization
	 * 
	 * @return DN string for organization
	 */
	public String getBaseDn() {
		return appConfiguration.getBaseDN();
	}

	public boolean isAllowPersonModification() {
		return appConfiguration.isAllowPersonModification(); // todo &&
																// configurationService.getConfiguration().getManageIdentityPermission()
																// !=
																// null
																// &&
																// configurationService.getConfiguration().getProfileManagment().isBooleanValue();
	}

	public GluuBoolean[] getBooleanSelectionTypes() {
		return new GluuBoolean[] { GluuBoolean.DISABLED, GluuBoolean.ENABLED };
	}

	public GluuBoolean[] getJavaBooleanSelectionTypes() {
		return new GluuBoolean[] { GluuBoolean.TRUE, GluuBoolean.FALSE };
	}

	public GluuStatus[] getActiveInactiveStatuses() {
		return new GluuStatus[] { GluuStatus.ACTIVE, GluuStatus.INACTIVE };
	}

	public ProgrammingLanguage[] getProgrammingLanguageTypes() {
		return new ProgrammingLanguage[] { ProgrammingLanguage.PYTHON, ProgrammingLanguage.JAVA_SCRIPT };
	}

	public LdapOxAuthConfiguration getOxAuthSetting(String configurationDn) {
		LdapOxAuthConfiguration ldapOxAuthConfiguration = null;
		try {
			configurationDn = configurationDn.replace("ou=oxtrust", "ou=oxauth");
			ldapOxAuthConfiguration = persistenceEntryManager.find(LdapOxAuthConfiguration.class, configurationDn);
			return ldapOxAuthConfiguration;
		} catch (BasePersistenceException ex) {
			log.error("Failed to load configuration from LDAP");
		}

		return null;
	}

	public void saveLdapOxAuthConfiguration(LdapOxAuthConfiguration ldapOxAuthConfiguration) {
		persistenceEntryManager.merge(ldapOxAuthConfiguration);
	}

	private BaseCacheService getCacheService() {
		if (appConfiguration.getUseLocalCache()) {
			return localCacheService;
		}

		return cacheService;
	}

}