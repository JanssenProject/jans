/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model.custom.script.type.uma;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;

/**
 * Dummy implementation of interface AuthorizationPolicyType
 *
 * @author Yuriy Movchan Date:01/13/2015
 */
public class DummyAuthorizationPolicyType implements AuthorizationPolicyType {

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
	public boolean authorize(Object authorizationContext, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

}
