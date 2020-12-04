/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.scope;

import java.util.List;
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for dynamic scope script
 *
 * @author Yuriy Movchan Date: 06/30/2015
 */
public interface DynamicScopeType extends BaseExternalType {

    boolean update(Object dynamicScopeContext, Map<String, SimpleCustomProperty> configurationAttributes);

    List<String> getSupportedClaims(Map<String, SimpleCustomProperty> configurationAttributes);

}
