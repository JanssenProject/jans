/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.scim;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

import java.util.Map;

/**
 * @author Val Pecaoco
 */
public interface ScimType extends BaseExternalType {

    boolean createUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postCreateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postUpdateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postDeleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean createGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postCreateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postUpdateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postDeleteGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean getUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean getGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

}
