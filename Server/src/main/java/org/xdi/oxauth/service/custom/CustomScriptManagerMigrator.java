/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom;

import java.util.Arrays;
import java.util.List;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.model.ProgrammingLanguage;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.config.CustomAuthenticationConfiguration;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.model.CustomScript;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.service.LdapCustomAuthenticationConfigurationService;
import org.xdi.service.custom.script.CustomScriptManager;
import org.xdi.util.INumGenerator;

/**
 * Provides actual versions of scrips
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Scope(ScopeType.APPLICATION)
@Name("customScriptManagerMigrator")
@AutoCreate
// Remove this class after CE 1.7 release
public class CustomScriptManagerMigrator {
	
	@In
	private CustomScriptManager customScriptManager;
	
	@In
	private CustomScriptService customScriptService;

	@Logger
    private Log log;

	private static final long serialVersionUID = -3225890597520443390L;

    public void migrateOldConfigurations() {
    	// Check if there are new configuration
		List<CustomScript> customScripts = customScriptService.findCustomScripts(customScriptManager.getSupportedCustomScriptTypes(), CustomScriptManager.CUSTOM_SCRIPT_CHECK_ATTRIBUTES);
		if (customScripts.size() > 0) {
			return;
		}
		
		// Load authentication configurations stored in old format
		LdapCustomAuthenticationConfigurationService ldapCustomAuthenticationConfigurationService = (LdapCustomAuthenticationConfigurationService) Component.getInstance(LdapCustomAuthenticationConfigurationService.class);
		List<CustomAuthenticationConfiguration> customAuthenticationConfigurations = ldapCustomAuthenticationConfigurationService.getCustomAuthenticationConfigurations();
		
		if (customAuthenticationConfigurations.size() == 0) {
			return;
		}
		
		String basedInum = ConfigurationFactory.instance().getConfiguration().getOrganizationInum();
		for (CustomAuthenticationConfiguration customAuthenticationConfiguration : customAuthenticationConfigurations) {
			String customScriptId = basedInum + "!" + INumGenerator.generate(2);
			String dn = customScriptService.buildDn(customScriptId);
			
			CustomScript customScript = new CustomScript();
			customScript.setDn(dn);
			customScript.setInum(customScriptId);
			customScript.setProgrammingLanguage(ProgrammingLanguage.PYTHON);
			customScript.setScriptType(CustomScriptType.PERSON_AUTHENTICATION);
			
			customScript.setName(customAuthenticationConfiguration.getName());
			customScript.setLevel(customAuthenticationConfiguration.getLevel());
			customScript.setEnabled(customAuthenticationConfiguration.isEnabled());
			customScript.setRevision(customAuthenticationConfiguration.getVersion());

			// Corect package name and base type
			String script = customAuthenticationConfiguration.getCustomAuthenticationScript();
			script = script.
					replaceAll("from org.xdi.oxauth.service.python.interfaces import", "from org.xdi.model.custom.script.type.auth import").
					replaceAll("ExternalAuthenticatorType", "PersonAuthenticationType").
					replaceAll("ExternalAuthenticator", "PersonAuthentication");
			customScript.setScript(script);

			List<SimpleCustomProperty> moduleProperties = Arrays.asList(new SimpleCustomProperty("usage_type", customAuthenticationConfiguration.getUsageType().toString()));
			customScript.setModuleProperties(moduleProperties);

			customScript.setConfigurationProperties(customAuthenticationConfiguration.getCustomAuthenticationAttributes());
			
			customScriptService.add(customScript);

			log.info("Successfully imported '{0}' authentication script", customScript.getName());
		}
    }

    public static CustomScriptManagerMigrator instance() {
        return (CustomScriptManagerMigrator) Component.getInstance(CustomScriptManagerMigrator.class);
    }

}
