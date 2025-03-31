/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.user;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.model.bind.BindCredentials;

/**
 * Dummy implementation of interface LinkInterceptionType
 *
 * @author Yuriy Movchan Date: 12/30/2014
 */
public class DummyLinkInterceptionType implements LinkInterceptionType {

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}
	@Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }
	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public int getApiVersion() {
		return 2;
	}

	@Override
	public boolean updateUser(Object person, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

    @Override
    public BindCredentials getBindCredentials(String configId, Map<String, SimpleCustomProperty> configurationAttributes) {
        return null;
    }

	@Override
	public boolean isStartProcess(Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

}
