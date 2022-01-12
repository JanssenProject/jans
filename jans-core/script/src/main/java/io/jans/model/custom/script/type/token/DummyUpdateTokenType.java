/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.model.custom.script.type.token;

import java.util.Map;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * @author Yuriy Movchan
 */
public class DummyUpdateTokenType implements UpdateTokenType {

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
	public boolean modifyIdToken(Object jwr, Object tokenContext) {
		return false;
	}

    @Override
    public boolean modifyRefreshToken(Object refreshToken, Object tokenContext) {
        return false;
    }

    @Override
    public boolean modifyAccessToken(Object accessToken, Object tokenContext) {
        return false;
    }

    @Override
    public int getRefreshTokenLifetimeInSeconds(Object tokenContext) {
        return 0;
    }

    @Override
    public int getIdTokenLifetimeInSeconds(Object context) {
        return 0;
    }

    @Override
    public int getAccessTokenLifetimeInSeconds(Object context) {
        return 0;
    }
}