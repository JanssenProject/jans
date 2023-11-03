/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.server.service;

import io.jans.lock.model.config.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with organization
 *
 * @author Yuriy Movchan Date: 09/07/2023
 */
@ApplicationScoped
public class OrganizationService extends io.jans.lock.service.OrganizationService {

	private static final long serialVersionUID = 4502134792415981865L;

	@Inject
    private AppConfiguration appConfiguration;

	public String getAppConfigurationBaseDn() {
		return appConfiguration.getBaseDN();
	}

}
