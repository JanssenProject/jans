/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.client;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

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
	public boolean createClient(Object context) {
		return false;
	}

	@Override
	public boolean updateClient(Object context) {
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

    @Override
    public String getDcrHmacSecret(Object context) {
        return "";
    }

    @Override
    public String getDcrJwks(Object context) {
        return "";
    }

    @Override
    public boolean isCertValidForClient(Object cert, Object context) {
        return false;
    }

    @Override
    public boolean modifyPutResponse(Object responseAsJsonObject, Object executionContext) {
        return false;
    }

    @Override
    public boolean modifyReadResponse(Object responseAsJsonObject, Object executionContext) {
        return false;
    }

    @Override
    public boolean modifyPostResponse(Object responseAsJsonObject, Object executionContext) {
        return false;
    }
}
