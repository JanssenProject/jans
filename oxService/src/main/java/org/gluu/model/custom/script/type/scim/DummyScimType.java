/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.scim;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

/**
 * @author Val Pecaoco
 */
public class DummyScimType implements ScimType {

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
    public boolean createUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postCreateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean updateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postUpdateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean deleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postDeleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean createGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postCreateGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean updateGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postUpdateGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean deleteGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean postDeleteGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

}
