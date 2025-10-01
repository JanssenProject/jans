/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.fido2;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

public class DummyFido2ExtensionType implements Fido2ExtensionType {
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
    public boolean registerAttestationStart(Object paramAsJsonNode, Object context) {
        return false;
    }

	@Override
	public boolean registerAttestationFinish(Object paramAsJsonNode, Object context) {
		return false;
	}

    @Override
    public boolean verifyAttestationStart(Object paramAsJsonNode, Object context) {
        return false;
    }

	@Override
	public boolean verifyAttestationFinish(Object paramAsJsonNode, Object context) {
		return false;
	}

    @Override
    public boolean authenticateAssertionStart(Object paramAsJsonNode, Object context) {
        return false;
    }

	@Override
	public boolean authenticateAssertionFinish(Object paramAsJsonNode, Object context) {
		return false;
	}

    @Override
    public boolean verifyAssertionStart(Object paramAsJsonNode, Object context) {
        return false;
    }

	@Override
	public boolean verifyAssertionFinish(Object paramAsJsonNode, Object context) {
		return false;
	}

}
