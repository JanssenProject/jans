package io.jans.link.server.service;

import io.jans.link.model.config.AppConfiguration;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * Provides operations with organization
 *
 * @author Yuriy Movchan Date: 09/07/2023
 */
@ApplicationScoped
@Priority(Interceptor.Priority.APPLICATION + 5)
public class OrganizationService extends io.jans.link.service.OrganizationService {

	private static final long serialVersionUID = 4502134792415981865L;

	@Inject
    private AppConfiguration appConfiguration;

	public String getAppConfigurationBaseDn() {
		return appConfiguration.getBaseDN();
	}

}
