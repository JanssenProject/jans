/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.service;

import io.jans.model.ApplicationType;

/**
 * Provides operations with organization
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
public abstract class OrganizationService extends io.jans.service.OrganizationService {

	private static final long serialVersionUID = -1959146007518514678L;

	public String getDnForOrganization() {
		return getDnForOrganization(getAppConfigurationBaseDn());
	}

	public abstract String getAppConfigurationBaseDn();

	/**
	 * Build DN string for organization
	 * 
	 * @return DN string for organization
	 */
	public String getBaseDn() {
		return getAppConfigurationBaseDn();
	}

	@Override
	public ApplicationType getApplicationType() {
		return null;
	}


}