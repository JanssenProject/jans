/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.conf;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Custom script configuration
 *
 * @author Yuriy Movchan Date: 12/04/2014
 */
public class CustomScriptConfiguration {
    private CustomScript customScript;
    private BaseExternalType externalType;
    private Map<String, SimpleCustomProperty> configurationAttributes;

    public CustomScriptConfiguration(CustomScript customScript, BaseExternalType externalType,
            Map<String, SimpleCustomProperty> configurationAttributes) {
        this.customScript = customScript;
        this.externalType = externalType;
        this.configurationAttributes = configurationAttributes;
    }

    public String getInum() {
        return customScript.getInum();
    }

    public CustomScript getCustomScript() {
        return customScript;
    }

    public BaseExternalType getExternalType() {
        return externalType;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public String getName() {
        return this.customScript.getName();
    }

    public int getLevel() {
        return this.customScript.getLevel();
    }
    
    @Override
    public String toString() {
        return "CustomScriptConfiguration [customScript=" + customScript + ", externalType=" + externalType + ", configurationAttributes="
                + configurationAttributes
                + "]";
    }
    

}
