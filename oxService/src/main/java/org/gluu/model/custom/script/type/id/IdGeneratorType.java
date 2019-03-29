/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.custom.script.type.id;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external custom ID generation python script
 *
 * @author Yuriy Movchan Date: 01/16/2015
 */
public interface IdGeneratorType extends BaseExternalType {

    String generateId(String appId, String idType, String idPrefix, Map<String, SimpleCustomProperty> configurationAttributes);

}
