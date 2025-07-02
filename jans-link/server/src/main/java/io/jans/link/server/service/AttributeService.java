package io.jans.link.server.service;

import io.jans.link.model.config.AppConfiguration;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * Provides operations with attributes
 *
 * @author Yuriy Movchan Date: 09/07/2023
 */
@ApplicationScoped
@Priority(Interceptor.Priority.APPLICATION + 5)
public class AttributeService extends io.jans.link.service.AttributeService {

	private static final long serialVersionUID = 3502134792415981865L;

	@Inject
    private AppConfiguration appConfiguration;

    public String getPersonCustomObjectClass() {
        return appConfiguration.getPersonCustomObjectClass();
    }

    public String[] getPersonObjectClassTypes() {
        return appConfiguration.getPersonObjectClassTypes();
    }

}
