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
import org.xdi.oxauth.service.custom.interfaces.BaseExternalType;

/**
 * Base interface for external authentication python script
 *
 * @author Yuriy Movchan Date: 08/21/2012
 */
public interface CustomAuthenticatorType extends BaseExternalType {

	public boolean isValidAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes);

	public String getAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes);

	public boolean authenticate(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step);

	public boolean prepareForStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step);
	
	public int getCountAuthenticationSteps(Map<String, SimpleCustomProperty> configurationAttributes);

	public String getPageForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step);

	public List<String> getExtraParametersForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step);

	public boolean logout(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters);

}
