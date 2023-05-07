/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.fido2;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

public class DummyFido2InterceptionType implements Fido2InterceptionType {
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
    public boolean interceptRegisterAttestation(Object paramAsJsonNode, Object context) {
        return false;
    }

    @Override
    public boolean interceptVerifyAttestation(Object paramAsJsonNode, Object context) {
        return false;
    }

    @Override
    public boolean interceptAuthenticateAssertion(Object paramAsJsonNode, Object context) {
        return false;
    }

    @Override
    public boolean interceptVerifyAssertion(Object paramAsJsonNode, Object context) {
        return false;
    }
}
