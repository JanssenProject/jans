/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.service.custom.script;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.model.custom.script.type.BaseExternalType;
import org.gluu.service.custom.inject.ReloadScript;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * Provides factory methods needed to create external extension
 *
 * @author Yuriy Movchan Date: 01/08/2015
 */
public class ExternalScriptService implements Serializable {

    private static final long serialVersionUID = -1070021905117441202L;

    @Inject
    protected Logger log;

    @Inject
    protected CustomScriptManager customScriptManager;

    protected CustomScriptType customScriptType;

    protected Map<String, CustomScriptConfiguration> customScriptConfigurationsNameMap;
    protected List<CustomScriptConfiguration> customScriptConfigurations;
    protected CustomScriptConfiguration defaultExternalCustomScript;

    public ExternalScriptService(CustomScriptType customScriptType) {
        this.customScriptType = customScriptType;
    }

    public void reload(@Observes @ReloadScript String event) {
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
            // Determine default script. It has lower level than others
            if ((defaultExternalCustomScript == null) || (defaultExternalCustomScript.getLevel() >= customScriptConfiguration.getLevel())) {
                defaultExternalCustomScript = customScriptConfiguration;
            }
        }

        return defaultExternalCustomScript;
    }

    public int executeExternalGetApiVersion(CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.debug("Executing python 'getApiVersion' authenticator method");
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

}
