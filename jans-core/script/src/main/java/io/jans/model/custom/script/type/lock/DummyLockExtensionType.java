/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.model.custom.script.type.lock;

import java.util.List;
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Base interface for external lock python script
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
public class DummyLockExtensionType implements LockExtensionType {
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
	public void beforeDataPut(Object messageNode, Object dataNode, Object context) {
	}

	@Override
	public void beforeDataRemoval(Object messageNode, Object context) {
	}

	@Override
	public void beforePolicyPut(String sourceUri, List<String> policies, Object context) {
	}

	@Override
	public void beforePolicyRemoval(String sourceUri, Object context) {
	}

}
