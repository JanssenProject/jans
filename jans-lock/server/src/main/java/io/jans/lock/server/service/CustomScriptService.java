/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.server.service;

import io.jans.lock.model.config.StaticConfiguration;
import io.jans.service.custom.script.AbstractCustomScriptService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 5)
public class CustomScriptService extends AbstractCustomScriptService {

	@Inject
	private StaticConfiguration staticConfiguration;

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScripts();
    }

}
