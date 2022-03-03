/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.script;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.jans.service.custom.inject.ReloadScript;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Provides factory methods needed to create external extension
 *
 * @author Yuriy Movchan Date: 01/08/2015
 */
public abstract class ExternalScriptService implements Serializable {

    private static final long serialVersionUID = -1070021905117441202L;

    @Inject
    protected Logger log;

    @Inject
    protected CustomScriptManager customScriptManager;

    protected CustomScriptType customScriptType;
    
    protected boolean loaded;

    protected Map<String, CustomScriptConfiguration> customScriptConfigurationsNameMap;
    protected List<CustomScriptConfiguration> customScriptConfigurations;
    protected CustomScriptConfiguration defaultExternalCustomScript;

    @PostConstruct
    public void init() {
    	this.loaded = false;
    }

    public ExternalScriptService(CustomScriptType customScriptType) {
        this.customScriptType = customScriptType;
    }

    /**
     * Method for standalone usage
     */
	public void configure(StandaloneCustomScriptManager customScriptManager) {
		this.customScriptManager = customScriptManager;
		this.log = LoggerFactory.getLogger(ExternalScriptService.class);
	}

    public void reload(@Observes @ReloadScript String event) {
    	// Skip reload if global script is not enabled for this application
    	if (!customScriptManager.isSupportedType(customScriptType)) {
    		return;
    	}
        // Get actual list of external configurations
        List<CustomScriptConfiguration> newCustomScriptConfigurations = customScriptManager
                .getCustomScriptConfigurationsByScriptType(customScriptType);
        addExternalConfigurations(newCustomScriptConfigurations);

        this.customScriptConfigurations = newCustomScriptConfigurations;
        this.customScriptConfigurationsNameMap = buildExternalConfigurationsNameMap(customScriptConfigurations);

        // Determine default configuration
        this.defaultExternalCustomScript = determineDefaultCustomScriptConfiguration(this.customScriptConfigurations);

        // Allow to execute additional logic
        reloadExternal();
        
        loaded = true;
    }

    protected void addExternalConfigurations(List<CustomScriptConfiguration> newCustomScriptConfigurations) {
    }

    protected void reloadExternal() {
    }

    private Map<String, CustomScriptConfiguration> buildExternalConfigurationsNameMap(List<CustomScriptConfiguration> customScriptConfigurations) {
        Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(
                customScriptConfigurations.size());

        for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
            reloadedExternalConfigurations.put(StringHelper.toLowerCase(customScriptConfiguration.getName()), customScriptConfiguration);
        }

        return reloadedExternalConfigurations;
    }

    public CustomScriptConfiguration determineDefaultCustomScriptConfiguration(List<CustomScriptConfiguration> customScriptConfigurations) {
        CustomScriptConfiguration defaultExternalCustomScript = null;
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            // Determine default script. It has bigger level than others
            if ((defaultExternalCustomScript == null) || (defaultExternalCustomScript.getLevel() < customScriptConfiguration.getLevel())) {
                defaultExternalCustomScript = customScriptConfiguration;
            }
        }

        return defaultExternalCustomScript;
    }

    public int executeExternalGetApiVersion(CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.trace("Executing python 'getApiVersion' authenticator method");
            BaseExternalType externalAuthenticator = (BaseExternalType) customScriptConfiguration.getExternalType();
            return externalAuthenticator.getApiVersion();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return -1;
    }

    public void saveScriptError(CustomScript customScript, Exception exception) {
        customScriptManager.saveScriptError(customScript, exception);
    }

    public void clearScriptError(CustomScript customScript) {
        customScriptManager.clearScriptError(customScript);
    }

    public boolean isEnabled() {
        if (this.customScriptConfigurations == null) {
            return false;
        }

        return this.customScriptConfigurations.size() > 0;
    }

    public CustomScriptConfiguration getCustomScriptConfigurationByName(String name) {
        return this.customScriptConfigurationsNameMap.get(StringHelper.toLowerCase(name));
    }

    public CustomScriptConfiguration getDefaultExternalCustomScript() {
        return defaultExternalCustomScript;
    }

    public List<CustomScriptConfiguration> getCustomScriptConfigurations() {
        return this.customScriptConfigurations;
    }

    public List<CustomScriptConfiguration> getCustomScriptConfigurationsByDns(List<String> dns) {
        if (dns == null || dns.isEmpty() || customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            return Lists.newArrayList();
        }
        List<CustomScriptConfiguration> scripts = Lists.newArrayList();
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (dns.contains(script.getCustomScript().getDn())) {
                scripts.add(script);
            }
        }
        return scripts;
    }

	public CustomScriptType getCustomScriptType() {
		return customScriptType;
	}

	public boolean isLoaded() {
		return loaded;
	}

}
