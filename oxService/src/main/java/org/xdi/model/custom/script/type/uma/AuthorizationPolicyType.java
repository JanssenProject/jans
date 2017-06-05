/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.custom.script.type.uma;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.type.BaseExternalType;

import java.util.Map;

/**
 * Base interface for external custom UMA authorization policy python script
 *
 * @author Yuriy Movchan Date: 01/13/2015
 */
@Deprecated // remove after full UMA2 move
public interface AuthorizationPolicyType extends BaseExternalType {

	public boolean authorize(Object authorizationContext, Map<String, SimpleCustomProperty> configurationAttributes);

}
