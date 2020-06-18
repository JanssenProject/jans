/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.custom.script.type.scope;

import java.util.List;
import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * Base interface for dynamic scope script
 *
 * @author Yuriy Movchan Date: 06/30/2015
 */
public interface DynamicScopeType extends BaseExternalType {

    boolean update(Object dynamicScopeContext, Map<String, SimpleCustomProperty> configurationAttributes);

    List<String> getSupportedClaims(Map<String, SimpleCustomProperty> configurationAttributes);

}
