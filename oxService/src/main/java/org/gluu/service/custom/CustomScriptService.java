package org.gluu.service.custom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.service.OrganizationService;
import org.gluu.service.custom.script.AbstractCustomScriptService;

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
