/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.model.custom.script.type.ssa;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

public class DummyModifySsaResponseType implements ModifySsaResponseType {

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
    public boolean create(Object jwr, Object tokenContext) {
        return false;
    }

    @Override
    public boolean get(Object jsonArray, Object ssaContext) {
        return true;
    }

    @Override
    public boolean revoke(Object ssaList, Object ssaContext) {
        return false;
    }
}