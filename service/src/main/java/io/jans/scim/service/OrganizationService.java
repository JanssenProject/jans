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

import io.jans.config.oxtrust.AppConfiguration;
import io.jans.config.oxtrust.LdapOxAuthConfiguration;
import io.jans.model.GluuStatus;
import io.jans.model.ProgrammingLanguage;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.base.GluuBoolean;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.ArrayHelper;
import io.jans.util.StringHelper;
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
public class OrganizationService extends io.jans.service.OrganizationService {

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
	 * Build DN string for organization
	 * 
	 * @return DN string for organization
	 */
	public String getBaseDn() {
		return appConfiguration.getBaseDN();
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