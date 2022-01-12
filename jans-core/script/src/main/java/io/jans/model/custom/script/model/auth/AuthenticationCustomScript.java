/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.model.auth;

import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

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
