/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model.custom.script.model.auth;

import java.util.ArrayList;
import java.util.List;

import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.model.CustomScript;
import org.xdi.util.StringHelper;

/**
 * Custom authentication script configuration 
 *
 * @author Yuriy Movchan Date: 12/29/2014
 */
public class AuthenticationCustomScript extends CustomScript {

	public AuthenticationCustomScript() {}

	public AuthenticationCustomScript(CustomScript customScript) {
		super(customScript);
	}

	public AuthenticationScriptUsageType getUsageType() {
		AuthenticationScriptUsageType usageType = null;
		for (SimpleCustomProperty moduleProperty : getModuleProperties()) {
			if (StringHelper.equalsIgnoreCase(moduleProperty.getValue1(), "usage_type")) {
				usageType = AuthenticationScriptUsageType.getByValue(moduleProperty.getValue2());
				break;
			}
		}

		return usageType;
	}

	public void setUsageType(AuthenticationScriptUsageType usageType) {
		List<SimpleCustomProperty> moduleProperties = getModuleProperties();
		
		if (moduleProperties == null) {
			moduleProperties = new ArrayList<SimpleCustomProperty>();
			setModuleProperties(moduleProperties);
		}

		SimpleCustomProperty usageTypeModuleProperties = new SimpleCustomProperty("usage_type", usageType.getValue());
		moduleProperties.add(usageTypeModuleProperties);
	}

}
