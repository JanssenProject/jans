/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.custom.script.model.auth;

import org.gluu.model.AuthenticationScriptUsageType;
import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;

/**
 * Person authentication script configuration
 *
 * @author Yuriy Movchan Date: 12/29/2014
 */
public class AuthenticationCustomScript extends CustomScript {

    public static final String USAGE_TYPE_MODEL_PROPERTY = "usage_type";

    public AuthenticationCustomScript() {
    }

    public AuthenticationCustomScript(CustomScript customScript) {
        super(customScript);
    }

    public AuthenticationScriptUsageType getUsageType() {
        SimpleCustomProperty moduleProperty = getModuleProperty(USAGE_TYPE_MODEL_PROPERTY);
        AuthenticationScriptUsageType usageType = null;

        if (moduleProperty == null) {
            return usageType;
        }

        return AuthenticationScriptUsageType.getByValue(moduleProperty.getValue2());
    }

    public void setUsageType(AuthenticationScriptUsageType usageType) {
        setModuleProperty(USAGE_TYPE_MODEL_PROPERTY, usageType.getValue());
    }

}
