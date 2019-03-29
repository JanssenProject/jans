/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.custom.script.type.scope;

import java.util.List;
import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

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
