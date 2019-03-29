/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.custom.script.conf;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.model.custom.script.type.BaseExternalType;

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

}
