/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.custom.script.type.auth;

import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;

import java.util.List;
import java.util.Map;

/**
 * Dummy implementation of interface PersonAuthenticationType
 *
 * @author Yuriy Movchan Date: 08/21/2012
 */
public class DummyPersonAuthenticationType implements PersonAuthenticationType {

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean isValidAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public String getAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes) {
		return null;
	}

	@Override
	public boolean authenticate(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		return false;
	}

	@Override
	public int getNextStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		return -1;
	}

	@Override
	public boolean prepareForStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		return false;
	}

	@Override
	public List<String> getExtraParametersForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step) {
		return null;
	}

	@Override
	public int getCountAuthenticationSteps(Map<String, SimpleCustomProperty> configurationAttributes) {
		return 0;
	}

	@Override
	public String getPageForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step) {
		return null;
	}

	@Override
	public boolean logout(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters) {
		return false;
	}

	@Override
	public String getLogoutExternalUrl(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters) {
		return null;
	}

	@Override
	public int getApiVersion() {
		return 1;
	}

	@Override
	public Map<String, String> getAuthenticationMethodClaims() {
		return null;
	}

}
