/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.user;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

/**
 * Dummy implementation of interface UserRegistrationType
 *
 * @author Yuriy Movchan Date: 01/16/2015
 */
public class DummyUserRegistrationType implements UserRegistrationType {

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
    public boolean initRegistration(Object user, Map<String, String[]> requestParameters, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean preRegistration(Object user, Map<String, String[]> requestParameters, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postRegistration(Object user, Map<String, String[]> requestParameters, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean confirmRegistration(Object user, Map<String, String[]> requestParameters,
            Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

}
