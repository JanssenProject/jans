/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.model.custom.script.type.scim;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.type.BaseExternalType;

import java.util.Map;

/**
 * @author Val Pecaoco
 */
public interface ScimType extends BaseExternalType {

    boolean createUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean createGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteGroup(Object user, Map<String, SimpleCustomProperty> configurationAttributes);
}
