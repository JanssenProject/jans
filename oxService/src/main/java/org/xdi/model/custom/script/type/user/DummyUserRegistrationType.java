/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.model.custom.script.type.user;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;

/**
 * Dummy implementation of interface UserRegistrationType
 *
 * @author Yuriy Movchan Date: 12/30/2014
 */
public class DummyUserRegistrationType implements UserRegistrationType {

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public int getApiVersion() {
		return 1;
	}

	@Override
	public boolean updateUser(Object person, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

}
