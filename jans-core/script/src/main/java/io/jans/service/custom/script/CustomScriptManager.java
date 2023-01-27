/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.script;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

import io.jans.service.custom.inject.ReloadScript;
import org.apache.commons.io.FileUtils;
import io.jans.model.ScriptLocationType;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.SimpleExtendedCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.cdi.event.UpdateScriptEvent;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

/**
 * Provides actual versions of scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@ApplicationScoped
public class CustomScriptManager {

	public static final String CUSTOM_SCRIPT_MODIFIED_EVENT_TYPE = "customScriptModifiedEvent";
	public static final int DEFAULT_INTERVAL = 30; // 30 seconds

	protected static final String[] CUSTOM_SCRIPT_CHECK_ATTRIBUTES = { "dn", "inum", "jansRevision", "jansScrTyp",
			"jansModuleProperty", "jansEnabled" };

	@Inject
	protected Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	protected ExternalTypeCreator externalTypeCreator;

	@Inject
	protected AbstractCustomScriptService customScriptService;

	@Inject
	@ReloadScript
	private Event<String> event;

	@Inject
	private Instance<ExternalScriptService> externalScriptServiceInstance;

	protected List<CustomScriptType> supportedCustomScriptTypes;
	private Map<String, CustomScriptConfiguration> customScriptConfigurations;

	private AtomicBoolean isActive;
	private long lastFinishedTime;

	private Map<CustomScriptType, List<CustomScriptConfiguration>> customScriptConfigurationsByScriptType;

	@Asynchronous
	public void initTimer(List<CustomScriptType> supportedCustomScriptTypes) {
		this.supportedCustomScriptTypes = supportedCustomScriptTypes;

		configure();

		final int delay = 30;

		reload(true);

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, DEFAULT_INTERVAL), new UpdateScriptEvent(),
				Scheduled.Literal.INSTANCE));
	}

	protected void configure() {
		this.isActive = new AtomicBoolean(false);
		this.lastFinishedTime = System.currentTimeMillis();
	}

	@SuppressWarnings("java:S1181")
	public void reloadTimerEvent(@Observes @Scheduled UpdateScriptEvent updateScriptEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reload(false);
		} catch (Throwable ex) {
			log.error("Exception happened while reloading custom scripts configuration", ex);
		} finally {
			this.isActive.set(false);
			this.lastFinishedTime = System.currentTimeMillis();
			log.trace("Last finished time '{}'", new Date(this.lastFinishedTime));
		}
	}

	@SuppressWarnings("java:S1172")
	public void destroy(@BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
		log.debug("Destroying custom scripts configurations");
		if (this.customScriptConfigurations == null) {
			return;
		}

		// Destroy authentication methods
		for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurations
				.entrySet()) {
			destroyCustomScript(customScriptConfigurationEntry.getValue());
		}
	}

	private void reload(boolean syncUpdate) {
		boolean modified = reloadImpl();

		if (modified) {
			updateScriptServices(syncUpdate);
		}
	}

	protected void updateScriptServices(boolean syncUpdate) {
		if (syncUpdate) {
			for (ExternalScriptService externalScriptService : externalScriptServiceInstance) {
				if (supportedCustomScriptTypes.contains(externalScriptService.getCustomScriptType())) {
					externalScriptService.reload(CUSTOM_SCRIPT_MODIFIED_EVENT_TYPE);
				}
			}
		} else {
			event.fire(CUSTOM_SCRIPT_MODIFIED_EVENT_TYPE);
		}
	}

	private boolean reloadImpl() {
		// Load current script revisions
		List<CustomScript> customScripts;
		if (supportedCustomScriptTypes.isEmpty()) {
			customScripts = new ArrayList<>();
		} else {
			customScripts = customScriptService.findCustomScripts(supportedCustomScriptTypes,
					CUSTOM_SCRIPT_CHECK_ATTRIBUTES);
		}

		// Store updated external authenticator configurations
		ReloadResult reloadResult = reloadCustomScriptConfigurations(this.customScriptConfigurations, customScripts);
		this.customScriptConfigurations = reloadResult.getCustomScriptConfigurations();

		// Group external authenticator configurations by usage type
		this.customScriptConfigurationsByScriptType = groupCustomScriptConfigurationsByScriptType(
				this.customScriptConfigurations);

		return reloadResult.isModified();
	}

	private class ReloadResult {
		private Map<String, CustomScriptConfiguration> customScriptConfigurations;
		private boolean modified;

		ReloadResult(Map<String, CustomScriptConfiguration> customScriptConfigurations, boolean modified) {
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
			Map<String, CustomScriptConfiguration> customScriptConfigurations, List<CustomScript> newCustomScripts) {
		Map<String, CustomScriptConfiguration> newCustomScriptConfigurations;

		boolean modified = false;

		if (customScriptConfigurations == null) {
			newCustomScriptConfigurations = new HashMap<>();
			modified = true;
		} else {
			// Clone old map to avoid reload not changed scripts because it's time and CPU
			// consuming process
			newCustomScriptConfigurations = new HashMap<>(customScriptConfigurations);
		}

		List<String> newSupportedCustomScriptInums = new ArrayList<>();
		for (CustomScript newCustomScript : newCustomScripts) {
			if (!newCustomScript.isEnabled()) {
				continue;
			}

			if (ScriptLocationType.FILE == newCustomScript.getLocationType()) {
				// Add to script revision file modification time. This should allow to
				// reload script automatically after changing location_type
				long fileModifiactionTime = getFileModificationTime(newCustomScript.getLocationPath());

				newCustomScript.setRevision(newCustomScript.getRevision() + fileModifiactionTime);
			}

			String newSupportedCustomScriptInum = StringHelper.toLowerCase(newCustomScript.getInum());
			newSupportedCustomScriptInums.add(newSupportedCustomScriptInum);

			CustomScriptConfiguration prevCustomScriptConfiguration = newCustomScriptConfigurations
					.get(newSupportedCustomScriptInum);
			if (prevCustomScriptConfiguration == null || prevCustomScriptConfiguration.getCustomScript()
                    .getRevision() != newCustomScript.getRevision()) {
				// Destroy old version properly before creating new one
				if (prevCustomScriptConfiguration != null) {
					destroyCustomScript(prevCustomScriptConfiguration);
				}

				// Load script entry with all attributes
				CustomScript loadedCustomScript = customScriptService.getCustomScriptByDn(
						newCustomScript.getScriptType().getCustomScriptModel(), newCustomScript.getDn());

				// Prepare configuration attributes
				Map<String, SimpleCustomProperty> newConfigurationAttributes = new HashMap<>();

				List<SimpleExtendedCustomProperty> simpleCustomProperties = loadedCustomScript
						.getConfigurationProperties();
				if (simpleCustomProperties == null) {
					simpleCustomProperties = new ArrayList<>(0);

				}

				for (SimpleCustomProperty simpleCustomProperty : simpleCustomProperties) {
					newConfigurationAttributes.put(simpleCustomProperty.getValue1(), simpleCustomProperty);
				}

				if (ScriptLocationType.FILE == loadedCustomScript.getLocationType()) {
					// Add to script revision file modification time. This should allow to
					// reload script automatically after changing location_type
					long fileModifiactionTime = getFileModificationTime(loadedCustomScript.getLocationPath());
					loadedCustomScript.setRevision(newCustomScript.getRevision() + fileModifiactionTime);

					if (fileModifiactionTime != 0) {
						String scriptFromFile = loadFromFile(loadedCustomScript.getLocationPath());
						if (StringHelper.isNotEmpty(scriptFromFile)) {
							loadedCustomScript.setScript(scriptFromFile);
						}

					}
				}
				
				// Load script
				BaseExternalType newCustomScriptExternalType = externalTypeCreator.createExternalType(loadedCustomScript,
						newConfigurationAttributes);

				CustomScriptConfiguration newCustomScriptConfiguration = new CustomScriptConfiguration(
						loadedCustomScript, newCustomScriptExternalType, newConfigurationAttributes);

				// Store configuration and script
				newCustomScriptConfigurations.put(newSupportedCustomScriptInum, newCustomScriptConfiguration);

				modified = true;
			}
		}

		// Remove old external scripts configurations
		for (Iterator<Entry<String, CustomScriptConfiguration>> it = newCustomScriptConfigurations.entrySet()
				.iterator(); it.hasNext();) {
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

	private String loadFromFile(String locationPath) {
		try {
			String scriptFromFile = FileUtils.readFileToString(new File(locationPath), StandardCharsets.UTF_8);
			if (log.isTraceEnabled()) {
                log.trace("Loaded from file: {}, script: {}", locationPath, scriptFromFile);
            }
			return scriptFromFile;
		} catch (IOException ex) {
			log.error("Failed to load script from '{}'", locationPath);
		}

		return null;
	}

	private long getFileModificationTime(String locationPath) {
		File scriptFile = new File(locationPath);

		if (scriptFile.exists()) {
			return scriptFile.lastModified();
		}

		return 0;
	}

	private boolean destroyCustomScript(CustomScriptConfiguration customScriptConfiguration) {
		String customScriptInum = customScriptConfiguration.getInum();

		boolean result = executeCustomScriptDestroy(customScriptConfiguration);
		if (!result) {
			log.error("Failed to destroy custom script '{}' correctly", customScriptInum);
		}

		return result;
	}

	private Map<CustomScriptType, List<CustomScriptConfiguration>> groupCustomScriptConfigurationsByScriptType(
			Map<String, CustomScriptConfiguration> customScriptConfigurations) {
		Map<CustomScriptType, List<CustomScriptConfiguration>> newCustomScriptConfigurationsByScriptType = new EnumMap<>(CustomScriptType.class);

		for (CustomScriptType customScriptType : this.supportedCustomScriptTypes) {
			List<CustomScriptConfiguration> customConfigurationsByScriptType = new ArrayList<>();
			newCustomScriptConfigurationsByScriptType.put(customScriptType, customConfigurationsByScriptType);
		}

		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations.values()) {
			CustomScriptType customScriptType = customScriptConfiguration.getCustomScript().getScriptType();
			List<CustomScriptConfiguration> customConfigurationsByScriptType = newCustomScriptConfigurationsByScriptType
					.get(customScriptType);
			if (customConfigurationsByScriptType != null) {
				customConfigurationsByScriptType.add(customScriptConfiguration);
			}
		}

		return newCustomScriptConfigurationsByScriptType;
	}


	public boolean executeCustomScriptDestroy(CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'destroy' custom script method");
			BaseExternalType externalType = customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration
					.getConfigurationAttributes();
			return externalType.destroy(configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}

		return false;
	}

	public CustomScriptConfiguration getCustomScriptConfigurationByInum(String inum) {
		return this.customScriptConfigurations.get(inum);
	}

	public List<CustomScriptConfiguration> getCustomScriptConfigurationsByScriptType(
			CustomScriptType customScriptType) {
		List<CustomScriptConfiguration> tmpCustomScriptConfigurationsByScriptType = this.customScriptConfigurationsByScriptType
				.get(customScriptType);
		if (tmpCustomScriptConfigurationsByScriptType == null) {
			tmpCustomScriptConfigurationsByScriptType = new ArrayList<>(0);
		}
		return new ArrayList<>(tmpCustomScriptConfigurationsByScriptType);
	}

	public List<CustomScriptConfiguration> getCustomScriptConfigurations() {
		return new ArrayList<>(this.customScriptConfigurations.values());
	}

	public List<CustomScriptType> getSupportedCustomScriptTypes() {
		return supportedCustomScriptTypes;
	}

	public boolean isSupportedType(CustomScriptType customScriptType) {
		return supportedCustomScriptTypes.contains(customScriptType);
	}

    public void saveScriptError(CustomScript customScript, Exception exception) {
        externalTypeCreator.saveScriptError(customScript, exception);
    }

    public void clearScriptError(CustomScript customScript) {
        externalTypeCreator.clearScriptError(customScript);
    }

}
