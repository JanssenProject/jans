package org.xdi.oxauth.service.python.interfaces;

import java.util.List;
import java.util.Map;

import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;

/**
 * Base interface for external authentication python script
 *
 * @author Yuriy Movchan Date: 08.21.2012
 */
public interface ExternalAuthenticatorType {

	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes);

	public boolean isValidAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes);

	public String getAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, Map<String, SimpleCustomProperty> configurationAttributes);

	public boolean authenticate(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step);

	public boolean prepareForStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step);
	
	public int getCountAuthenticationSteps(Map<String, SimpleCustomProperty> configurationAttributes);

	public String getPageForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step);

	public List<String> getExtraParametersForStep(Map<String, SimpleCustomProperty> configurationAttributes, int step);

	public boolean logout(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters);

	public int getApiVersion();

}
