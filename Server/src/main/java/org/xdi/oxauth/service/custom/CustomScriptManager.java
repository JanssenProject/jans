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
import org.jboss.beans.metadata.api.annotations.Destroy;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
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
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.config.CustomAuthenticationConfiguration;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.uma.persistence.ProgrammingLanguage;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.LdapCustomAuthenticationConfigurationService;
import org.xdi.oxauth.service.custom.conf.CustomScript;
import org.xdi.oxauth.service.custom.conf.CustomScriptType;
import org.xdi.oxauth.service.custom.interfaces.BaseExternalType;
import org.xdi.oxauth.service.custom.model.CustomScriptConfiguration;
import org.xdi.service.PythonService;
import org.xdi.util.INumGenerator;
import org.xdi.util.StringHelper;

/**
 * Provides actual versions of scrips
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Scope(ScopeType.APPLICATION)
@Name("customScriptManager")
public class CustomScriptManager implements Serializable {

	private static final long serialVersionUID = -4225890597520443390L;

	private final static String EVENT_TYPE = "CustomScriptHolderTimerEvent";
	public final static String MODIFIED_EVENT_TYPE = "CustomScriptModifiedEvent";
    private final static int DEFAULT_INTERVAL = 30; // 30 seconds
    
    private final static String[] CUSTOM_SCRIPT_CHECK_ATTRIBUTES = { "dn", "inum", "oxRevision", "gluuStatus" };

	@Logger
	private Log log;

	@In
	private PythonService pythonService;
	
	@In(value = "customScriptService")
	private AbstractCustomScriptService customScriptService;

	private List<CustomScriptType> supportedCustomScriptTypes;
	private Map<String, CustomScriptConfiguration> customScriptConfigurations;

	private AtomicBoolean isActive;
	private long lastFinishedTime;

	private Map<CustomScriptType, List<CustomScriptConfiguration>> customScriptConfigurationsByScriptType;

    public void init(List<CustomScriptType> supportedCustomScriptTypes) {
		this.supportedCustomScriptTypes = supportedCustomScriptTypes;

		this.isActive = new AtomicBoolean(false);
		this.lastFinishedTime = System.currentTimeMillis();

		reload();

		Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(1 * 60 * 1000L, DEFAULT_INTERVAL * 1000L));
    }

    // Remove this method after CE 1.7 release
    public void migrateOldConfigurations() {
    	// Check if there are new configuration
		List<CustomScript> customScripts = customScriptService.findCustomScripts(supportedCustomScriptTypes, CUSTOM_SCRIPT_CHECK_ATTRIBUTES);
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

	@Observer(EVENT_TYPE)
	@Asynchronous
	public void reloadTimerEvent() {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reload();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading custom scripts configuration", ex);
		} finally {
			this.isActive.set(false);
			this.lastFinishedTime = System.currentTimeMillis();
			log.trace("Last finished time '{0}'", new Date(this.lastFinishedTime));
		}
	}

	@Destroy
	public void destroy() {
		log.debug("Destroying custom scripts configurations");
		if (this.customScriptConfigurations == null) {
			return;
		}

		// Destroy authentication methods
		for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurations.entrySet()) {
			destroyCustomScript(customScriptConfigurationEntry.getValue());
		}
	}

	private void reload() {
		boolean modified = reloadImpl();
		
		if (modified) {
			Events.instance().raiseEvent(MODIFIED_EVENT_TYPE);
		}
	}

	private boolean reloadImpl() {
		// Load current script revisions
		List<CustomScript> customScripts = customScriptService.findCustomScripts(supportedCustomScriptTypes, CUSTOM_SCRIPT_CHECK_ATTRIBUTES);

		// Store updated external authenticator configurations
		ReloadResult reloadResult = reloadCustomScriptConfigurations(this.customScriptConfigurations, customScripts);
		this.customScriptConfigurations = reloadResult.getCustomScriptConfigurations();

		// Group external authenticator configurations by usage type
		this.customScriptConfigurationsByScriptType = groupCustomScriptConfigurationsByScriptType(this.customScriptConfigurations);
		
		return reloadResult.isModified();
	}
	
	private class ReloadResult {
		private Map<String, CustomScriptConfiguration> customScriptConfigurations;
		private boolean modified;

		public ReloadResult(Map<String, CustomScriptConfiguration> customScriptConfigurations, boolean modified) {
			this.customScriptConfigurations = customScriptConfigurations;
			this.modified = modified;
		}

		public Map<String, CustomScriptConfiguration> getCustomScriptConfigurations() {
			return customScriptConfigurations;
		}

		public boolean isModified() {
			return modified;
		}
		
	}

	private ReloadResult reloadCustomScriptConfigurations(
			Map<String,  CustomScriptConfiguration> customScriptConfigurations, List<CustomScript> newCustomScripts) {
		Map<String, CustomScriptConfiguration> newCustomScriptConfigurations;
		
		boolean modified = false;

		if (customScriptConfigurations == null) {
			newCustomScriptConfigurations = new HashMap<String, CustomScriptConfiguration>();
		} else {
			// Clone old map to avoid reload not changed scripts becuase it's time and CPU consuming process
			newCustomScriptConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations);
		}

		List<String> newSupportedCustomScriptInums = new ArrayList<String>();
		for (CustomScript newCustomScript : newCustomScripts) {
	        if (!newCustomScript.isEnabled()) {
	        	continue;
	        }
	        	
			String newSupportedCustomScriptInum = StringHelper.toLowerCase(newCustomScript.getInum());
			newSupportedCustomScriptInums.add(newSupportedCustomScriptInum);

			CustomScriptConfiguration prevCustomScriptConfiguration = newCustomScriptConfigurations.get(newSupportedCustomScriptInum);
			if ((prevCustomScriptConfiguration == null) || (prevCustomScriptConfiguration.getCustomScript().getRevision() != newCustomScript.getRevision())) {
				// Destroy old version properly before creating new one
				if (prevCustomScriptConfiguration != null) {
					destroyCustomScript(prevCustomScriptConfiguration);
				}
				
				// Load script entry with all attributes
				CustomScript loadedCustomScript = customScriptService.getCustomScriptByDn(newCustomScript.getDn());

				// Prepare configuration attributes
				Map<String, SimpleCustomProperty> newConfigurationAttributes = new HashMap<String, SimpleCustomProperty>();
				for (SimpleCustomProperty simpleCustomProperty : loadedCustomScript.getConfigurationProperties()) {
					newConfigurationAttributes.put(simpleCustomProperty.getValue1(), simpleCustomProperty);
				}

				// Create authenticator
	        	BaseExternalType newCustomScriptExternalType = createExternalType(loadedCustomScript, newConfigurationAttributes);

	        	CustomScriptConfiguration newCustomScriptConfiguration = new CustomScriptConfiguration(loadedCustomScript, newCustomScriptExternalType, newConfigurationAttributes);

				// Store configuration and authenticator
				newCustomScriptConfigurations.put(newSupportedCustomScriptInum, newCustomScriptConfiguration);

				modified = true;
			}
		}

		// Remove old external authenticator configurations
		for (Iterator<Entry<String, CustomScriptConfiguration>> it = newCustomScriptConfigurations.entrySet().iterator(); it.hasNext();) {
			Entry<String, CustomScriptConfiguration> externalAuthenticatorConfigurationEntry = it.next();

			String prevSupportedCustomScriptInum = externalAuthenticatorConfigurationEntry.getKey();

			if (!newSupportedCustomScriptInums.contains(prevSupportedCustomScriptInum)) {
				// Destroy old authentication method
				destroyCustomScript(externalAuthenticatorConfigurationEntry.getValue());
				it.remove();

				modified = true;
			}
		}

		return new ReloadResult(newCustomScriptConfigurations, modified);
	}

	private boolean destroyCustomScript(CustomScriptConfiguration customScriptConfiguration) {
		String customScriptInum = customScriptConfiguration.getInum();

		boolean result = executeCustomScriptDestroy(customScriptConfiguration);
		if (!result) {
			log.error("Failed to destroy custom script '{0}' correctly", customScriptInum);
		}
		
		return result;
	}

	private Map<CustomScriptType, List<CustomScriptConfiguration>> groupCustomScriptConfigurationsByScriptType(Map<String, CustomScriptConfiguration> customScriptConfigurations) {
		Map<CustomScriptType, List<CustomScriptConfiguration>> newCustomScriptConfigurationsByScriptType = new HashMap<CustomScriptType, List<CustomScriptConfiguration>>();
		
		for (CustomScriptType customScriptType : this.supportedCustomScriptTypes) {
			List<CustomScriptConfiguration> customConfigurationsByScriptType = new ArrayList<CustomScriptConfiguration>();
			newCustomScriptConfigurationsByScriptType.put(customScriptType, customConfigurationsByScriptType);
		}

		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations.values()) {
			CustomScriptType customScriptType = customScriptConfiguration.getCustomScript().getScriptType();
			List<CustomScriptConfiguration> customConfigurationsByScriptType = newCustomScriptConfigurationsByScriptType.get(customScriptType);
			customConfigurationsByScriptType.add(customScriptConfiguration);
		}
		
		return newCustomScriptConfigurationsByScriptType;
	}

	private BaseExternalType createExternalType(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
		String customScriptInum = customScript.getInum();

		BaseExternalType externalType;
		try {
			externalType = createExternalTypeFromStringWithPythonException(customScript, configurationAttributes);
		} catch (PythonException ex) {
			log.error("Failed to prepare external type '{0}'", ex, customScriptInum);
			return null;
		}

		if (externalType == null) {
			log.debug("Using default external type class");
			externalType = customScript.getScriptType().getDefaultImplementation();
		}

		return externalType;
	}

	public BaseExternalType createExternalTypeFromStringWithPythonException(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) throws PythonException {
		String script = customScript.getScript();
		if (script == null) {
			return null;
		}

		CustomScriptType customScriptType = customScript.getScriptType();
		BaseExternalType externalType = null;

		InputStream bis = null;
		try {
            bis = new ByteArrayInputStream(script.getBytes(Util.UTF8_STRING_ENCODING));
            externalType = pythonService.loadPythonScript(bis, customScriptType.getPythonClass(),
            		customScriptType.getCustomScriptType(), new PyObject[] { new PyLong(System.currentTimeMillis()) });
		} catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        } finally {
			IOUtils.closeQuietly(bis);
		}

		if (externalType == null) {
			return null;
		}

		boolean initialized = false;
		try {
			initialized = externalType.init(configurationAttributes);
		} catch (Exception ex) {
            log.error("Failed to initialize custom script", ex);
		}

		if (initialized) {
			return externalType;
		}
		
		return null;
	}

	public boolean executeCustomScriptDestroy(CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'destroy' custom script method");
			BaseExternalType externalType = customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalType.destroy(configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		return false;
	}

	public CustomScriptConfiguration getCustomScriptConfigurationByInum(String inum) {
		return this.customScriptConfigurations.get(inum);
	}

	public List<CustomScriptConfiguration> getCustomScriptConfigurationsByScriptType(CustomScriptType customScriptType) {
		return new ArrayList<CustomScriptConfiguration>(this.customScriptConfigurationsByScriptType.get(customScriptType));
	}

	public List<CustomScriptConfiguration> getCustomScriptConfigurations() {
		return new ArrayList<CustomScriptConfiguration>(this.customScriptConfigurations.values());
	}

    public static CustomScriptManager instance() {
        return (CustomScriptManager) Component.getInstance(CustomScriptManager.class);
    }

}
