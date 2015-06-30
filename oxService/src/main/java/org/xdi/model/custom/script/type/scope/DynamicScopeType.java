/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.custom.script.type.scope;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.type.BaseExternalType;

/**
 * Base interface for dynamic scope script
 *
 * @author Yuriy Movchan Date: 06/30/2015
 */
public interface DynamicScopeType extends BaseExternalType {

	public boolean update(Object dynamicScopeContext, Map<String, SimpleCustomProperty> configurationAttributes);

}
