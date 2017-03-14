package org.xdi.oxauth.service.external.internal;

import java.security.Identity;
import java.util.Map;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.type.auth.DummyPersonAuthenticationType;
import org.xdi.oxauth.service.UserService;

/**
 * Wrapper to call internal authentication method
 *
 * @author Yuriy Movchan Date: 06/04/2015
 */
public class InternalDefaultPersonAuthenticationType extends DummyPersonAuthenticationType {

	private UserService userService;

	public InternalDefaultPersonAuthenticationType() {
		this.userService = UserService.instance();
	}

	@Override
	public boolean authenticate(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		Credentials credentials = Identity.instance().getCredentials();
		if (credentials == null) {
			return false;
		}

		return userService.authenticate(credentials.getUsername(), credentials.getPassword());
	}

	@Override
	public boolean prepareForStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
		if (step == 1) {
			return true;
		}
		
		return super.prepareForStep(configurationAttributes, requestParameters, step);
	}

	@Override
	public int getCountAuthenticationSteps(Map<String, SimpleCustomProperty> configurationAttributes) {
		return 1;
	}

	@Override
	public boolean logout(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters) {
		return true;
	}

}
