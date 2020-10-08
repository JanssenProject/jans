/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.model.custom.script.type.idp;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for IDP script
 *
 * @author Yuriy Movchan Date: 06/18/2020
 */
public interface IdpType extends BaseExternalType {

	boolean translateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

	boolean updateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

}
