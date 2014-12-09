/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom.interfaces.client;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;

/**
 * Dummy implementation of interface ClientRegistrationType
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public class DummyClientRegistrationType implements ClientRegistrationType {

	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	public int getApiVersion() {
		return 1;
	}

}
