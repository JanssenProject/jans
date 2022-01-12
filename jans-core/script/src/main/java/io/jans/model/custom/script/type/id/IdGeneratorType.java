/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.id;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external custom ID generation python script
 *
 * @author Yuriy Movchan Date: 01/16/2015
 */
public interface IdGeneratorType extends BaseExternalType {

    String generateId(String appId, String idType, String idPrefix, Map<String, SimpleCustomProperty> configurationAttributes);

}
