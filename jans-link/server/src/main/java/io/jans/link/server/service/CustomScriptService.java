package io.jans.link.server.service;

import io.jans.link.model.config.StaticConfiguration;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 09/07/2023
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 5)
public class CustomScriptService extends io.jans.link.service.custom.CustomScriptService {

	@Inject
	private StaticConfiguration staticConfiguration;

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScripts();
    }

}
