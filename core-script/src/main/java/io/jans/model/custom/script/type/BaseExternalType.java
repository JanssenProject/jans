/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package io.jans.model.custom.script.type;

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
