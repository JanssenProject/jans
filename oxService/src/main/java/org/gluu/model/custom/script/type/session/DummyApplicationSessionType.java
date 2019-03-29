/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.session;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

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

}
