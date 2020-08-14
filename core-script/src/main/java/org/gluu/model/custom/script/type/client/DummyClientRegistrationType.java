/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.custom.script.type.client;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;

import java.util.Map;
/**
 * Dummy implementation of interface ClientRegistrationType
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public class DummyClientRegistrationType implements ClientRegistrationType {

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
		return 1;
	}

	@Override
	public boolean createClient(Object registerRequest, Object client, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

	@Override
	public boolean updateClient(Object registerRequest, Object client, Map<String, SimpleCustomProperty> configurationAttributes) {
		return false;
	}

    @Override
    public String getSoftwareStatementHmacSecret(Object context) {
        return "";
    }

    @Override
    public String getSoftwareStatementJwks(Object context) {
        return "";
    }
}
