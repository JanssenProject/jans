/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom.interfaces.auth;

import java.util.List;
import java.util.Map;

import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;

/**
 * Dummy implementation of interface CustomAuthenticatorType
 *
 * @author Yuriy Movchan Date: 08/21/2012
 */
public class DummyCustomAuthenticatorType implements CustomAuthenticatorType {

	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	public boolean isValidAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	public String getAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes) {
		return null;
	}

	public boolean authenticate(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		return false;
	}

	public boolean prepareForStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		return false;
	}

	public List<String> getExtraParametersForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step) {
		return null;
	}

	public int getCountAuthenticationSteps(Map<String, SimpleCustomProperty> configurationAttributes) {
		return 0;
	}

	public String getPageForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step) {
		return null;
	}

	public boolean logout(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters) {
		return false;
	}

	public int getApiVersion() {
		return 1;
	}

}
