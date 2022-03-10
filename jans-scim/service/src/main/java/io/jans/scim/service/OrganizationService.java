/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

import io.jans.scim.model.conf.AppConfiguration;
import io.jans.config.oxtrust.LdapOxAuthConfiguration;
import io.jans.model.ApplicationType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.scim.model.GluuOrganization;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;

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

	private BaseCacheService getCacheService() {
		if (appConfiguration.getUseLocalCache()) {
			return localCacheService;
		}

		return cacheService;
	}

	@Override
	public ApplicationType getApplicationType() {
		return ApplicationType.SCIM;
	}

}