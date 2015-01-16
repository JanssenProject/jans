/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.model.custom.script.type.user;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external update user python script
 *
 * @author Yuriy Movchan Date: 12/30/2012
 */
public interface UpdateUserType extends BaseExternalType {

	public boolean updateUser(Object user, boolean persisted, Map<String, SimpleCustomProperty> configurationAttributes);

}
