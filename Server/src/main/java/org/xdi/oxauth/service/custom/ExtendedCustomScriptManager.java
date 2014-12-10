/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.xdi.exception.PythonException;
import org.xdi.model.ProgrammingLanguage;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.config.CustomAuthenticationConfiguration;
import org.xdi.model.cusom.script.CustomScriptConfiguration;
import org.xdi.model.cusom.script.conf.CustomScript;
import org.xdi.model.cusom.script.type.BaseExternalType;
import org.xdi.model.cusom.script.type.CustomScriptType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.service.LdapCustomAuthenticationConfigurationService;
import org.xdi.service.PythonService;
import org.xdi.service.custom.script.CustomScriptManager;
import org.xdi.util.INumGenerator;
import org.xdi.util.StringHelper;

/**
 * Provides actual versions of scrips
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Scope(ScopeType.APPLICATION)
@Name("extendedCustomScriptManager")
@AutoCreate
// Remove this class after CE 1.7 release
public class ExtendedCustomScriptManager extends CustomScriptManager {

	private static final long serialVersionUID = -3225890597520443390L;

    public void migrateOldConfigurations() {
    	// Check if there are new configuration
		List<CustomScript> customScripts = customScriptService.findCustomScripts(this.supportedCustomScriptTypes, CUSTOM_SCRIPT_CHECK_ATTRIBUTES);
		if (customScripts.size() > 0) {
			return;
		}
		
		// Load authentication configurations stored in old format
		LdapCustomAuthenticationConfigurationService ldapCustomAuthenticationConfigurationService = (LdapCustomAuthenticationConfigurationService) Component.getInstance(LdapCustomAuthenticationConfigurationService.class);
		List<CustomAuthenticationConfiguration> customAuthenticationConfigurations = ldapCustomAuthenticationConfigurationService.getCustomAuthenticationConfigurations();
		
		if (customAuthenticationConfigurations.size() == 0) {
			return;
		}
		
		String basedInum = ConfigurationFactory.getConfiguration().getOrganizationInum();
		for (CustomAuthenticationConfiguration customAuthenticationConfiguration : customAuthenticationConfigurations) {
			String customScriptId = basedInum + "!" + INumGenerator.generate(2);
			String dn = customScriptService.buildDn(customScriptId);
			
			CustomScript customScript = new CustomScript();
			customScript.setDn(dn);
			customScript.setInum(customScriptId);
			customScript.setProgrammingLanguage(ProgrammingLanguage.PYTHON);
			customScript.setScriptType(CustomScriptType.CUSTOM_AUTHENTICATION);
			
			customScript.setName(customAuthenticationConfiguration.getName());
			customScript.setLevel(customAuthenticationConfiguration.getLevel());
			customScript.setEnabled(customAuthenticationConfiguration.isEnabled());
			customScript.setRevision(customAuthenticationConfiguration.getVersion());
			customScript.setScript(customAuthenticationConfiguration.getCustomAuthenticationScript());

			List<SimpleCustomProperty> moduleProperties = Arrays.asList(new SimpleCustomProperty("usage_type", customAuthenticationConfiguration.getUsageType().toString()));
			customScript.setModuleProperties(moduleProperties);

			customScript.setConfigurationProperties(customAuthenticationConfiguration.getCustomAuthenticationAttributes());
			
			customScriptService.add(customScript);

			log.info("Successfully imported '{0}' authentication script", customScript.getName());
		}
    }

    public static ExtendedCustomScriptManager instance() {
        return (ExtendedCustomScriptManager) Component.getInstance(ExtendedCustomScriptManager.class);
    }

}
