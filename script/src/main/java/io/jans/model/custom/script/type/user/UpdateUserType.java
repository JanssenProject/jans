/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.user;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external update user python script
 *
 * @author Yuriy Movchan Date: 12/30/2012
 */
public interface UpdateUserType extends BaseExternalType {

    boolean newUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postUpdateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean addUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postAddUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postDeleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

}
