/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.session;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Dummy implementation of interface ApplicationSessionType
 *
 * @author Yuriy Movchan Date: 12/30/2014
 */
public class DummyApplicationSessionType implements ApplicationSessionType {

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
    public boolean startSession(Object httpRequest, Object authorizationGrant, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean endSession(Object httpRequest, Object authorizationGrant, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public void onEvent(Object event) {
    }
}
