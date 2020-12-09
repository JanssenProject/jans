/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.scope;

import java.util.List;
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Dummy implementation of interface DynamicScopeType
 *
 * @author Yuriy Movchan Date: 06/30/2015
 */
public class DummyDynamicScopeType implements DynamicScopeType {

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
    public boolean update(Object dynamicScopeContext, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public List<String> getSupportedClaims(Map<String, SimpleCustomProperty> configurationAttributes) {
        return null;
    }

}
