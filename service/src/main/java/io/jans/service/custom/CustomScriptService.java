/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.jans.service.OrganizationService;
import io.jans.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 09/07/2020
 */
@ApplicationScoped
public class CustomScriptService extends AbstractCustomScriptService {
	
	private static final long serialVersionUID = -7670016078535552193L;

	@Inject
	private OrganizationService organizationService;

    public String baseDn() {
		return String.format("ou=scripts,%s", organizationService.getDnForOrganization(null));
    }

}
