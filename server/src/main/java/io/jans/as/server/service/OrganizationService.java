/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.as.model.configuration.AppConfiguration;

@ApplicationScoped
@Named("organizationService")
public class OrganizationService extends io.jans.as.common.service.OrganizationService {

	@Inject
	private AppConfiguration appConfiguration;

    protected boolean isUseLocalCache() {
    	return appConfiguration.getUseLocalCache();
    }

}
