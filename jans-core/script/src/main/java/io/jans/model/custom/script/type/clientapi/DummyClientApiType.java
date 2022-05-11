/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.clientapi;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DummyClientApiType implements ClientApiType {

    Logger log = LoggerFactory.getLogger(DummyClientApiType.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }

    @Override
    public boolean authorize(Object responseAsJsonObject, Object introspectionContext) {
        return false;
    }

}
