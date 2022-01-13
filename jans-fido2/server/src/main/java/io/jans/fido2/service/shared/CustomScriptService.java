/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.shared;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class CustomScriptService extends AbstractCustomScriptService {
	
	@Inject
	private StaticConfiguration staticConfiguration;

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScripts();
    }

}
