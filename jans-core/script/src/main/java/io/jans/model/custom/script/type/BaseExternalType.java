/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Base interface for external python script
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public interface BaseExternalType {

    boolean init(Map<String, SimpleCustomProperty> configurationAttributes);
    
    boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes);

    int getApiVersion();

}
