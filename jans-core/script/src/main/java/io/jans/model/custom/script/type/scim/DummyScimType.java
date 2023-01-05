/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.scim;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * @author jgomer2001
 */
public class DummyScimType implements ScimType {

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

    @Override
    public boolean getUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean getGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }
    
    @Override
    public boolean postSearchUsers(Object results, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }
    
    @Override
    public boolean postSearchGroups(Object results, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public Response manageResourceOperation(Object context, Object entity, Object payload, Map<String, SimpleCustomProperty> configurationAttributes) {
  	    return null;
    }

    @Override
    public Response manageSearchOperation(Object context, Object searchRequest, Map<String, SimpleCustomProperty> configurationAttributes) {
        return null;
    }

}
