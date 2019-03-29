/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.user;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

/**
 * Dummy implementation of interface UpdateUserType
 *
 * @author Yuriy Movchan Date: 12/30/2014
 *
 */
public class DummyUpdateUserType implements UpdateUserType {

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
    public boolean newUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean postUpdateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean updateUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean addUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean postAddUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean deleteUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean postDeleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

}
