/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service;

import io.jans.cacherefresh.model.config.AppConfiguration;
import io.jans.model.ApplicationType;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with organization
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@ApplicationScoped
@Priority(value = 5)
public class OrganizationService extends io.jans.service.OrganizationService {

	private static final long serialVersionUID = -1959146007518514678L;

	@Inject
	private AppConfiguration appConfiguration;

	public String getDnForOrganization() {
		return getDnForOrganization(appConfiguration.getBaseDN());
	}

	public String getDnForOrganization(String baseDn) {
		if (baseDn == null) {
			baseDn = "o=jans";
		}
		return baseDn;
	}

	/**
	 * Build DN string for organization
	 * 
	 * @return DN string for organization
	 */
	public String getBaseDn() {
		return appConfiguration.getBaseDN();
	}

	@Override
	public ApplicationType getApplicationType() {
		return ApplicationType.CACHE_REFRESH;
	}


}